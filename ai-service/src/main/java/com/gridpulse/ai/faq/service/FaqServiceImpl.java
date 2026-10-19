package com.gridpulse.ai.faq.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gridpulse.ai.faq.data.Faq;
import com.gridpulse.ai.faq.data.FaqEntity;
import com.gridpulse.ai.faq.data.FaqRepo;
import com.gridpulse.ai.faq.dto.FaqMatch;
import com.gridpulse.ai.faq.dto.FaqResponse;
import com.gridpulse.ai.gemini.GeminiService;
import com.gridpulse.ai.platform.exception.GeminiException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class FaqServiceImpl implements FaqService {

    private static final Set<String> STOP_WORDS = Set.of("a", "an", "the", "is", "are", "am", "was", "be", "to", "do",
            "does", "did", "i", "me", "my", "we", "you", "your", "it", "its", "of", "in", "on", "at", "for", "and", "or",
            "can", "could", "how", "what", "when", "where", "which", "who", "why", "with", "this", "that", "there",
            "please", "tell", "about", "gridpulse", "will", "would", "should", "if", "so", "any", "get");

    // one faq of faqs.json next to its db row
    private record SeedRow(FaqEntity entity, boolean isNew, boolean edited) {
    }

    // one faq with its score for the question
    private record Scored(FaqEntity faq, double score) {
    }

    // one word ending: when it fits + how many letters are cut off
    private record Ending(Predicate<String> fits, int cut) {
    }

    // tried from the top, only the first ending that fits is cut
    private static final List<Ending> ENDINGS = List.of(
            new Ending(word -> word.length() > 5 && word.endsWith("ings"), 4),
            new Ending(word -> word.length() > 5 && word.endsWith("ing"), 3),
            new Ending(word -> word.length() > 4 && word.endsWith("ed"), 2),
            new Ending(word -> word.length() > 3 && word.endsWith("s") && !word.endsWith("ss"), 1));

    private final FaqRepo faqRepo;
    private final GeminiService geminiService;
    private final ObjectMapper objectMapper;
    private final ChatMapper chatMapper;
    private final ChatbotProperties chatbotProperties;

    private volatile List<FaqEntity> knowledgeBase = List.of();

    // after startup, so a slow gemini call doesn't delay the server
    @Override
    @EventListener(ApplicationReadyEvent.class)
    public void seedFaqs() {
        boolean askGemini = geminiService.isEnabled();
        // every faq of the file next to its db row, an edited one already has the new text
        List<SeedRow> rows = readSeedFile().stream()
                .map(this::toSeedRow)
                .toList();
        List<FaqEntity> embedded = askGemini ? addVectors(rows) : List.of();
        // only new, edited or just embedded rows are saved, in file order
        rows.stream()
                .filter(row -> row.isNew() || row.edited() || embedded.contains(row.entity()))
                .forEach(row -> faqRepo.save(row.entity()));
        knowledgeBase = List.copyOf(faqRepo.findAll());

        long inserted = rows.stream().filter(SeedRow::isNew).count();
        long categories = knowledgeBase.stream().map(FaqEntity::getCategory).distinct().count();
        long withVectors = knowledgeBase.stream().filter(f -> f.getEmbedding() != null).count();
        log.info("FAQ knowledge base ready: {} faqs in {} categories ({} new, {} embedded now, {} with vectors)",
                knowledgeBase.size(), categories, inserted, embedded.size(), withVectors);
    }

    @Override
    public FaqMatch findBestMatch(String query, float[] queryEmbedding) {
        // nothing to search in or nothing asked = no match
        return Optional.ofNullable(query)
                .filter(text -> !knowledgeBase.isEmpty() && !text.isBlank())
                .map(text -> semanticOrLexical(text, queryEmbedding))
                .orElseGet(() -> new FaqMatch("none"));
    }

    // the vectors first, the word overlap when they find nothing good enough
    private FaqMatch semanticOrLexical(String query, float[] queryEmbedding) {
        return Optional.ofNullable(queryEmbedding)
                .map(this::semanticMatch)
                .filter(FaqMatch::matched)
                .orElseGet(() -> lexicalMatch(query));
    }

    private FaqMatch semanticMatch(float[] queryEmbedding) {
        // faqs with a vector of the same size only, the first best one wins a tie
        FaqMatch match = knowledgeBase.stream()
                .filter(faq -> faq.getEmbedding() != null && faq.getEmbedding().length == queryEmbedding.length)
                .map(faq -> new Scored(faq, cosine(queryEmbedding, faq.getEmbedding())))
                .filter(scored -> scored.score() > -1)
                .reduce((best, next) -> next.score() > best.score() ? next : best)
                .map(best -> new FaqMatch(best.faq(), best.score(), "semantic",
                        best.score() >= chatbotProperties.similarityThreshold()))
                .orElseGet(() -> new FaqMatch(null, -1, "semantic", false));
        // not close enough: the word overlap is tried next
        Optional.of(match)
                .filter(FaqMatch::matched)
                .ifPresentOrElse(found -> log.debug("semantic match faq {} ({})", found.faq().getId(),
                                String.format("%.3f", found.score())),
                        () -> log.debug("no semantic match above {} (best {}), trying lexical",
                                chatbotProperties.similarityThreshold(), match.score()));
        return match;
    }

    private FaqMatch lexicalMatch(String query) {
        Set<String> queryWords = tokens(query);
        // the faq sharing the most words wins, the first one on a tie
        FaqMatch match = knowledgeBase.stream()
                .map(faq -> new Scored(faq, jaccard(queryWords, tokens(faq.getQuestion() + " " + faq.getCategory()))))
                .filter(scored -> scored.score() > 0)
                .reduce((best, next) -> next.score() > best.score() ? next : best)
                .map(best -> new FaqMatch(best.faq(), best.score(), "lexical",
                        best.score() >= chatbotProperties.lexicalThreshold()))
                .orElseGet(() -> new FaqMatch(null, 0, "lexical", false));
        // no faq shares a word = null as the id
        log.debug("lexical best faq {} score {} matched {}",
                Optional.ofNullable(match.faq()).map(FaqEntity::getId).orElse(null),
                String.format("%.3f", match.score()), match.matched());
        return match;
    }

    // not in the db yet = a new row
    private SeedRow toSeedRow(Faq faq) {
        return faqRepo.findById(faq.id())
                .map(entity -> existingRow(entity, faq))
                .orElseGet(() -> new SeedRow(new FaqEntity(faq.id(), faq.category(), faq.question(), faq.answer()),
                        true, false));
    }

    private SeedRow existingRow(FaqEntity entity, Faq faq) {
        boolean edited = rowWasEdited(entity, faq);
        // an edited faq gets the text of the file
        Optional.of(entity)
                .filter(row -> edited)
                .ifPresent(row -> copyText(row, faq));
        return new SeedRow(entity, false, edited);
    }

    // rows without a vector ask gemini one by one, the ones that got it are returned
    // every key failed: no more gemini calls now, the other faqs get their vector at the next start
    private List<FaqEntity> addVectors(List<SeedRow> rows) {
        return rows.stream()
                .map(SeedRow::entity)
                .filter(entity -> entity.getEmbedding() == null)
                .takeWhile(this::addVector)
                .toList();
    }

    // faqs.json was edited: the old vector no longer fits, it is made again
    private void copyText(FaqEntity entity, Faq faq) {
        entity.setCategory(faq.category());
        entity.setQuestion(faq.question());
        entity.setAnswer(faq.answer());
        entity.setEmbedding(null);
    }

    private boolean rowWasEdited(FaqEntity entity, Faq faq) {
        return !Objects.equals(entity.getQuestion(), faq.question())
                || !Objects.equals(entity.getAnswer(), faq.answer())
                || !Objects.equals(entity.getCategory(), faq.category());
    }

    private boolean addVector(FaqEntity entity) {
        try {
            entity.setEmbedding(geminiService.embed(embeddingText(entity)));
            return true;
        } catch (GeminiException e) {
            log.warn("FAQ vectors stopped at faq {}, the rest are made at the next start: {}", entity.getId(),
                    e.getMessage());
            return false;
        }
    }

    @Override
    public List<FaqResponse> getAllFaqs() {
        return knowledgeBase.stream()
                .map(chatMapper::toResponse)
                .toList();
    }

    @Override
    public boolean hasEmbeddings() {
        return knowledgeBase.stream().anyMatch(f -> f.getEmbedding() != null);
    }

    private List<Faq> readSeedFile() {
        try (InputStream in = new ClassPathResource("faqs.json").getInputStream()) {
            return objectMapper.readValue(in, new TypeReference<List<Faq>>() { });
        } catch (IOException e) {
            log.error("faqs.json could not be read, chatbot starts with an empty knowledge base", e);
            return List.of();
        }
    }

    private String embeddingText(FaqEntity faq) {
        return faq.getCategory() + ": " + faq.getQuestion() + "\n" + faq.getAnswer();
    }

    private static double cosine(float[] a, float[] b) {
        // the float products added up one after the other
        double dot = IntStream.range(0, a.length)
                .mapToDouble(i -> a[i] * b[i])
                .reduce(0.0, Double::sum);
        // squared length of a, then of b
        double normA = IntStream.range(0, a.length)
                .mapToDouble(i -> a[i] * a[i])
                .reduce(0.0, Double::sum);
        double normB = IntStream.range(0, a.length)
                .mapToDouble(i -> b[i] * b[i])
                .reduce(0.0, Double::sum);
        // a vector of only zeros points nowhere
        return normA == 0 || normB == 0 ? 0 : dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private static double jaccard(Set<String> a, Set<String> b) {
        // words in both sets / words in either set, no words on one side = nothing in common
        long common = a.stream().filter(b::contains).count();
        long all = Stream.concat(a.stream(), b.stream()).distinct().count();
        return a.isEmpty() || b.isEmpty() ? 0 : (double) common / all;
    }

    // meaningful words only: "How do I LOG IN to my bookings?" -> { login, booking }
    private static Set<String> tokens(String text) {
        String normalised = text.toLowerCase()
                .replaceAll("\\blog\\s*in\\b", "login")
                .replaceAll("\\bsign\\s*in\\b", "login")
                .replaceAll("\\bsign\\s*up\\b", "signup")
                .replaceAll("\\blog\\s*out\\b", "logout")
                .replaceAll("\\be-mail\\b", "email")
                .replaceAll("\\bforgot(ten)?\\b", "forget");
        return Arrays.stream(normalised.split("[^a-z0-9+]+"))
                .filter(word -> !word.isBlank() && !STOP_WORDS.contains(word))
                .map(FaqServiceImpl::stem)
                .collect(Collectors.toSet());
    }

    private static String stem(String word) {
        // the first ending that fits is cut off
        String stem = ENDINGS.stream()
                .filter(ending -> ending.fits().test(word))
                .findFirst()
                .map(ending -> word.substring(0, word.length() - ending.cut()))
                .orElse(word);
        // then "ll" -> "l" on longer words
        return stem.length() > 5 && stem.endsWith("ll") ? stem.substring(0, stem.length() - 1) : stem;
    }
}
