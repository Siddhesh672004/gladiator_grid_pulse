package com.gridpulse.ai.copilot.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.stereotype.Component;

import com.google.genai.errors.ApiException;
import com.gridpulse.ai.gemini.GeminiKeys;
import com.gridpulse.ai.gemini.GeminiKeys.Problem;
import com.gridpulse.ai.platform.exception.GeminiException;

import lombok.RequiredArgsConstructor;

// one prompt (system text, the chat so far, the new message, the tools) through every gemini key in turn:
// a key that gives no answer is left for the next one at once (GeminiKeys). all failed -> GeminiException
// with a sentence for the user
@Component
@RequiredArgsConstructor
public class GeminiChatRunner {

    private static final String NO_KEY = "The Copilot needs a Gemini key. Please ask the admin to set GEMINI_API_KEY.";
    private static final String KEYS_NOT_VALID = "The AI keys are not valid. Please ask the admin to check them.";
    private static final String KEYS_USED_UP = "All AI keys have used up today's free requests (they come back at "
            + "05:30 IST). The pages of the app still work as usual.";
    private static final String AI_BUSY = "The AI service is busy right now. Please try again in a minute.";
    private static final String EMPTY_REPLY = "Sorry, I have no answer for that. Please ask again in other words.";

    private final List<ChatModel> geminiChatModels;
    private final GeminiKeys geminiKeys;
    private final PendingActions pendingActions;

    // one try with one key: reply null = the key gave no answer (problem says why)
    private record KeyTry(String reply, Problem problem) {
    }

    public String call(String system, List<Message> history, String message, Object... tools) {
        Optional.of(geminiKeys)
                .filter(keys -> keys.count() > 0)
                .orElseThrow(() -> new GeminiException(NO_KEY));
        List<Problem> problems = new ArrayList<>();
        int start = geminiKeys.current();
        // every key once, from the one in use: 1 -> 2 -> 3 -> 1
        return IntStream.range(0, geminiKeys.count())
                .map(i -> (start + i) % geminiKeys.count())
                .mapToObj(position -> tryKey(position, system, history, message, tools))
                .peek(attempt -> Optional.ofNullable(attempt.problem()).ifPresent(problems::add))
                .map(KeyTry::reply)
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new GeminiException(whyNoAnswer(problems)));
    }

    // google told this key to wait: skipped without a call
    private KeyTry tryKey(int position, String system, List<Message> history, String message, Object[] tools) {
        return Optional.ofNullable(geminiKeys.restingFor(position))
                .map(problem -> new KeyTry(null, problem))
                .orElseGet(() -> askKey(position, system, history, message, tools));
    }

    private KeyTry askKey(int position, String system, List<Message> history, String message, Object[] tools) {
        try {
            String reply = ChatClient.create(geminiChatModels.get(position)).prompt()
                    .system(system)
                    .messages(history)
                    .user(message)
                    .tools(tools)
                    .call()
                    .content();
            geminiKeys.use(position);
            return new KeyTry(Optional.ofNullable(reply).map(String::trim).filter(text -> !text.isBlank())
                    .orElse(EMPTY_REPLY), null);
        } catch (RuntimeException e) {
            // the actions this try prepared were never shown to anybody: gone, the next key starts clean
            pendingActions.discardPrepared();
            String reason = NestedExceptionUtils.getMostSpecificCause(e).getMessage();
            return new KeyTry(null, geminiKeys.useNext(position, httpStatus(e), reason));
        }
    }

    // gemini's http status inside spring ai's exception, 0 = no answer at all (timeout, no network)
    private int httpStatus(RuntimeException e) {
        return Stream.<Throwable>iterate(e, Objects::nonNull, Throwable::getCause)
                .filter(ApiException.class::isInstance)
                .map(ApiException.class::cast)
                .findFirst()
                .map(ApiException::code)
                .orElse(0);
    }

    // a busy or silent key may answer in a minute, a used up one tomorrow, a wrong one never
    private String whyNoAnswer(List<Problem> problems) {
        return Stream.of(Map.entry(problems.contains(Problem.BUSY) || problems.contains(Problem.NO_ANSWER), AI_BUSY),
                        Map.entry(problems.contains(Problem.NO_REQUESTS), KEYS_USED_UP))
                .filter(Map.Entry::getKey)
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(KEYS_NOT_VALID);
    }
}
