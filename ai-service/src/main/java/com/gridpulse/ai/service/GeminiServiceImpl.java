package com.gridpulse.ai.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gridpulse.ai.config.CacheConfig;
import com.gridpulse.ai.config.properties.GeminiProperties;
import com.gridpulse.ai.exceptions.GeminiException;

import jakarta.annotation.PostConstruct;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// plain HttpClient, no google sdk. the key goes in a header, never in the url
@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiServiceImpl implements GeminiService {

    private final GeminiProperties gemini;
    private final GeminiKeys geminiKeys;
    private final ObjectMapper objectMapper;

    private HttpClient httpClient;

    @PostConstruct
    public void init() {
        httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(gemini.timeoutSeconds())).build();
        // the log says whether the chatbot can use gemini
        Optional.of(geminiKeys)
                .filter(keys -> isEnabled())
                .ifPresentOrElse(keys -> log.info("Gemini enabled with {} key(s) (embedding: {}, generation: {})",
                                keys.count(), gemini.embeddingModel(), gemini.generationModel()),
                        () -> log.info("GEMINI_API_KEY not set - chatbot runs on lexical (word overlap) search only"));
    }

    @Override
    public boolean isEnabled() {
        return geminiKeys.count() > 0;
    }

    // the same words give the same vector: a day in redis saves gemini calls (and the daily quota)
    @Override
    @Cacheable(cacheNames = CacheConfig.EMBEDDINGS, key = "#text.trim().toLowerCase()")
    @CircuitBreaker(name = "gemini", fallbackMethod = "embedResting")
    public float[] embed(String text) {
        Map<String, Object> body = Map.of(
                "model", "models/" + gemini.embeddingModel(),
                "content", Map.of("parts", List.of(Map.of("text", text))));
        JsonNode values = Optional.of(call(gemini.embeddingModel() + ":embedContent", body)
                        .path("embedding").path("values"))
                .filter(numbers -> numbers.isArray() && !numbers.isEmpty())
                .orElseThrow(() -> new GeminiException("Gemini returned no embedding"));
        // the numbers of the answer, one after the other, as floats
        float[] vector = new float[values.size()];
        IntStream.range(0, values.size())
                .forEach(i -> vector[i] = (float) values.get(i).asDouble());
        log.trace("embedded {} chars -> {} dimensions", text.length(), vector.length);
        return vector;
    }

    @Override
    @CircuitBreaker(name = "gemini", fallbackMethod = "generateResting")
    public String generate(String prompt) {
        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", prompt)))),
                // thinkingBudget 0 = answer at once (token limit leaves room if the model thinks anyway)
                "generationConfig", Map.of("temperature", 0.3, "maxOutputTokens", 1000,
                        "thinkingConfig", Map.of("thinkingBudget", 0)));
        JsonNode parts = call(gemini.generationModel() + ":generateContent", body)
                .path("candidates").path(0).path("content").path("parts");
        // the text pieces of the answer put together, nothing in them = no answer
        return Optional.of(StreamSupport.stream(parts.spliterator(), false)
                        .map(part -> part.path("text").asText(""))
                        .collect(Collectors.joining()))
                .filter(text -> !text.isBlank())
                .map(String::trim)
                .orElseThrow(() -> new GeminiException("Gemini returned an empty answer"));
    }

    // breaker open (gemini failed a lot just now): the callers fall back to word-overlap search at once
    private float[] embedResting(String text, CallNotPermittedException e) {
        throw new GeminiException("Gemini is resting after too many failures");
    }

    private String generateResting(String prompt, CallNotPermittedException e) {
        throw new GeminiException("Gemini is resting after too many failures");
    }

    private JsonNode call(String modelAction, Object body) {
        Optional.of(geminiKeys)
                .filter(keys -> isEnabled())
                .orElseThrow(() -> new GeminiException("Gemini is not configured"));
        String json = toJson(body);
        int start = geminiKeys.current();
        // every key once, from the one in use: 1 -> 2 -> 3 -> 1. no answer -> the same request with the next key
        return IntStream.range(0, geminiKeys.count())
                .map(i -> (start + i) % geminiKeys.count())
                .filter(position -> geminiKeys.restingFor(position) == null)
                .mapToObj(position -> ask(modelAction, json, position))
                .flatMap(Optional::stream)
                .findFirst()
                .orElseThrow(() -> new GeminiException("Gemini " + modelAction + " failed with every key"));
    }

    // empty = this key gave no answer, GeminiKeys already moved on to the next one
    private Optional<JsonNode> ask(String modelAction, String json, int position) {
        try {
            HttpResponse<String> response = httpClient.send(request(modelAction, json, position),
                    HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            return status / 100 == 2 ? accepted(response.body(), position) : refused(position, status, response.body());
        } catch (IOException e) {
            // timeout, no network or an answer that is not json
            geminiKeys.useNext(position, 0, e.toString());
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GeminiException("Gemini call was interrupted", e);
        }
    }

    private Optional<JsonNode> accepted(String body, int position) throws IOException {
        JsonNode answer = objectMapper.readTree(body);
        geminiKeys.use(position);
        return Optional.of(answer);
    }

    private Optional<JsonNode> refused(int position, int status, String body) {
        geminiKeys.useNext(position, status, "HTTP " + status + ": " + errorMessage(body));
        return Optional.empty();
    }

    private HttpRequest request(String modelAction, String json, int position) {
        return HttpRequest.newBuilder()
                .uri(URI.create(gemini.baseUrl() + "/models/" + modelAction))
                .timeout(Duration.ofSeconds(gemini.timeoutSeconds()))
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", geminiKeys.key(position))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
    }

    private String toJson(Object body) {
        try {
            return objectMapper.writeValueAsString(body);
        } catch (IOException e) {
            throw new GeminiException("Gemini request could not be written", e);
        }
    }

    // google answers {"error": {"message": ...}}; anything else (a proxy's html page ...) is cut short
    private String errorMessage(String body) {
        String shortBody = body.substring(0, Math.min(body.length(), 200));
        try {
            String message = objectMapper.readTree(body).path("error").path("message").asText("");
            return message.isBlank() ? shortBody : message;
        } catch (IOException e) {
            return shortBody;
        }
    }
}
