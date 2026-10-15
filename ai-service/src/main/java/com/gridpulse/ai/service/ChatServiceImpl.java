package com.gridpulse.ai.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.gridpulse.ai.dto.FaqMatch;
import com.gridpulse.ai.dto.request.ChatRequest;
import com.gridpulse.ai.dto.response.ChatMessageResponse;
import com.gridpulse.ai.dto.response.ChatResponse;
import com.gridpulse.ai.entity.ChatMessage;
import com.gridpulse.ai.entity.FaqEntity;
import com.gridpulse.ai.exceptions.GeminiException;
import com.gridpulse.ai.mapper.ChatMapper;
import com.gridpulse.ai.repository.ChatMessageRepo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// the public faq chat: best faq (meaning vectors, then word overlap) -> gemini writes the reply from it
// no gemini -> the faq answer as it is. booking and the other changes are the logged-in Copilot's job
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private static final String FALLBACK_REPLY = "Sorry, I don't have an answer for that yet. I can help with your "
            + "account, race tracks, booking a session, the Live Race Control Board and feedback - try asking about "
            + "one of those. Log in to book or manage things right here in the chat.";

    // short messages with one of these words usually lean on the previous question
    private static final Set<String> FOLLOW_UP_WORDS = Set.of("it", "that", "this", "those", "these", "them", "they",
            "there", "one", "also", "else", "again", "then", "and", "what about", "how about");

    private final GeminiService geminiService;
    private final FaqService faqService;
    private final ConversationMemory conversationMemory;
    private final ChatMessageRepo chatMessageRepo;
    private final ChatMapper chatMapper;

    // the bot's answer + where it came from ("gemini", "faq" or "fallback")
    private record Reply(String text, String source) {
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        // a chat without an id gets a new one
        String sessionId = Optional.ofNullable(request.sessionId())
                .filter(id -> !id.isBlank())
                .map(String::trim)
                .orElseGet(() -> UUID.randomUUID().toString());
        // no key in secrets.properties: word search in the faq list, the answer as it is
        return answerFromFaqs(sessionId, request.message().trim(), geminiService.isEnabled());
    }

    // useGemini = false -> no gemini call at all: word overlap picks the faq and its answer goes out as it is
    private ChatResponse answerFromFaqs(String sessionId, String message, boolean useGemini) {
        List<ConversationMemory.Turn> history = conversationMemory.getHistory(sessionId);

        String resolvedQuestion = resolveQuestion(message, history, useGemini);
        FaqMatch match = findFaq(message, resolvedQuestion, useGemini);
        // a faq below the threshold counts as no faq
        FaqEntity faq = Optional.of(match)
                .filter(FaqMatch::matched)
                .map(FaqMatch::faq)
                .orElse(null);
        Reply reply = writeReply(faq, history, resolvedQuestion, useGemini);

        double confidence = Math.round(match.score() * 100.0) / 100.0;
        // the score is kept whenever a faq was looked at, even one below the threshold
        chatMessageRepo.save(new ChatMessage(sessionId, message, reply.text(),
                Optional.ofNullable(faq).map(FaqEntity::getId).orElse(null),
                Optional.ofNullable(match.faq()).map(looked -> confidence).orElse(null)));
        conversationMemory.addTurn(sessionId, new ConversationMemory.Turn(message, resolvedQuestion, reply.text()));
        log.info("chat {}: faq {} via {} ({}), reply from {}", sessionId,
                Optional.ofNullable(faq).<Object>map(FaqEntity::getId).orElse("none"), match.method(), confidence,
                reply.source());

        // a matched faq also tells the window which question and category it was
        return Optional.ofNullable(faq)
                .map(found -> new ChatResponse(reply.text(), true, found.getQuestion(), found.getCategory(), confidence,
                        reply.source(), sessionId, resolvedQuestion))
                .orElseGet(() -> new ChatResponse(reply.text(), reply.source(), sessionId, resolvedQuestion));
    }

    // gemini on -> compare meaning vectors; off (or it failed) -> compare words
    private FaqMatch findFaq(String message, String resolvedQuestion, boolean useGemini) {
        float[] queryEmbedding = (useGemini && faqService.hasEmbeddings()) ? embed(resolvedQuestion) : null;
        FaqMatch match = faqService.findBestMatch(resolvedQuestion, queryEmbedding);

        // word search on a joined follow-up: the new words alone often match better
        return Optional.of(message)
                .filter(typed -> queryEmbedding == null && !resolvedQuestion.equals(typed))
                .map(typed -> faqService.findBestMatch(typed, null))
                .filter(FaqMatch::matched)
                .orElse(match);
    }

    // null = no vector, the word search takes over
    private float[] embed(String question) {
        try {
            return geminiService.embed(question);
        } catch (GeminiException e) {
            log.warn("Embedding failed, using lexical search: {}", e.getMessage());
            return null;
        }
    }

    // no faq -> the fallback text, no gemini -> the faq answer as it is
    private Reply writeReply(FaqEntity faq, List<ConversationMemory.Turn> history, String question,
            boolean useGemini) {
        return Optional.ofNullable(faq)
                .map(found -> useGemini ? geminiReply(found, history, question) : new Reply(found.getAnswer(), "faq"))
                .orElseGet(() -> new Reply(FALLBACK_REPLY, "fallback"));
    }

    private Reply geminiReply(FaqEntity faq, List<ConversationMemory.Turn> history, String question) {
        try {
            return new Reply(geminiService.generate(groundedPrompt(faq, history, question)), "gemini");
        } catch (GeminiException e) {
            log.warn("Generation failed, sending the faq answer as it is: {}", e.getMessage());
            return new Reply(faq.getAnswer(), "faq");
        }
    }

    @Override
    public List<ChatMessageResponse> getHistory(String sessionId) {
        return chatMessageRepo.findBySessionIdOrderByCreatedAtAscIdAsc(sessionId).stream()
                .map(chatMapper::toResponse)
                .toList();
    }

    @Override
    public void clearMemory(String sessionId) {
        conversationMemory.clear(sessionId);
    }

    // a follow-up ("what about cancelling it?") becomes a question that stands on its own
    private String resolveQuestion(String message, List<ConversationMemory.Turn> history, boolean useGemini) {
        // the first message of a chat has nothing to lean on
        return history.isEmpty() ? message : resolveFollowUp(message, history, useGemini);
    }

    // gemini rewrites it first; no gemini (or no usable question from it) -> the local follow-up rule
    private String resolveFollowUp(String message, List<ConversationMemory.Turn> history, boolean useGemini) {
        return Optional.of(useGemini)
                .filter(Boolean::booleanValue)
                .map(on -> geminiRewrite(message, history))
                .orElseGet(() -> joinLocally(message, history));
    }

    // without gemini: put the last question in front
    private String joinLocally(String message, List<ConversationMemory.Turn> history) {
        return Optional.of(message)
                .filter(this::looksLikeFollowUp)
                .map(followUp -> {
                    String previous = history.get(history.size() - 1).resolvedQuestion();
                    String combined = previous + " " + followUp;
                    log.debug("follow-up joined locally: '{}' -> '{}'", followUp, combined);
                    return combined;
                })
                .orElse(message);
    }

    // null = gemini gave no usable question, the local follow-up rule is used
    private String geminiRewrite(String message, List<ConversationMemory.Turn> history) {
        String answer;
        try {
            answer = geminiService.generate(rewritePrompt(message, history));
        } catch (GeminiException e) {
            log.warn("Rewrite failed, using the local follow-up rule: {}", e.getMessage());
            return null;
        }
        // first line only, without quotes, at most 300 characters
        String rewritten = Optional.of(answer.lines().findFirst().orElse(message).replaceAll("^[\"']|[\"']$", "")
                        .trim())
                .filter(question -> !question.isBlank() && question.length() <= 300)
                .orElse(null);
        Optional.ofNullable(rewritten)
                .ifPresent(question -> log.debug("follow-up rewritten by gemini: '{}' -> '{}'", message, question));
        return rewritten;
    }

    // a short message (8 words at most) with a word like "it" or "that" leans on the last question
    private boolean looksLikeFollowUp(String message) {
        String lower = message.toLowerCase().replaceAll("[^a-z ]", " ").trim();
        String padded = " " + lower + " ";
        return lower.split("\\s+").length <= 8
                && FOLLOW_UP_WORDS.stream().anyMatch(word -> padded.contains(" " + word + " "));
    }

    private String rewritePrompt(String message, List<ConversationMemory.Turn> history) {
        StringBuilder prompt = new StringBuilder("Rewrite the user's LAST message as one standalone question about the "
                + "GridPulse motorsport booking app, using the conversation for context. If it already stands on its "
                + "own, return it unchanged. Reply with the question only.\n\nConversation:\n");
        appendHistory(prompt, history);
        prompt.append("\nLast message: ").append(message);
        return prompt.toString();
    }

    private String groundedPrompt(FaqEntity faq, List<ConversationMemory.Turn> history, String question) {
        StringBuilder prompt = new StringBuilder("You are the GridPulse assistant for a motorsport track booking and "
                + "live race-control web app. Answer the user's question using ONLY the FAQ below. If the FAQ does not "
                + "cover the question, say you are not sure and suggest asking about accounts, tracks, bookings, the "
                + "Live Race Control Board or feedback. Be friendly, plain text, at most 80 words.\n\n");
        prompt.append("FAQ (").append(faq.getCategory()).append(")\nQ: ").append(faq.getQuestion())
                .append("\nA: ").append(faq.getAnswer()).append("\n\n");
        // the earlier turns only when there are some
        Optional.of(history)
                .filter(turns -> !turns.isEmpty())
                .ifPresent(turns -> {
                    prompt.append("Conversation so far:\n");
                    appendHistory(prompt, turns);
                    prompt.append('\n');
                });
        prompt.append("User question: ").append(question);
        return prompt.toString();
    }

    // oldest turn first, two lines per turn
    private void appendHistory(StringBuilder prompt, List<ConversationMemory.Turn> history) {
        history.stream()
                .map(turn -> "User: " + turn.userMessage() + "\nAssistant: " + turn.botReply() + "\n")
                .forEach(prompt::append);
    }
}
