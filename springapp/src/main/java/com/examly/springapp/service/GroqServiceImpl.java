package com.examly.springapp.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.examly.springapp.config.properties.GroqProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// same idea as GeminiServiceImpl. only the http status is logged, never the key or the feedback text
@Slf4j
@Service
@RequiredArgsConstructor
public class GroqServiceImpl implements GroqService {

    private final GroqProperties groq;
    private final ObjectMapper objectMapper;

    private HttpClient httpClient;

    @PostConstruct
    public void init() {
        httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(groq.timeoutSeconds())).build();
        // no key: the server still starts, the log says feedback gets no label
        Optional.of(groq)
                .filter(GroqProperties::hasKey)
                .ifPresentOrElse(settings -> log.info("Groq enabled (model: {})", settings.model()),
                        () -> log.info("GROQ_API_KEY not set - feedback is saved without an AI label"));
    }

    @Override
    public boolean isEnabled() {
        return groq.hasKey();
    }

    @Override
    public String ask(String instructions, String text) {
        // no key -> "" without a call
        return isEnabled() ? send(instructions, text) : "";
    }

    private String send(String instructions, String text) {
        try {
            HttpResponse<String> response = httpClient.send(buildRequest(instructions, text),
                    HttpResponse.BodyHandlers.ofString());
            // only a 200 has an answer, anything else gives "" and its status in the log
            return response.statusCode() == 200 ? readAnswer(response.body()) : refused(response.statusCode());
        } catch (IOException e) {
            log.warn("Groq could not be reached: {}", e.getMessage());
            return "";
        } catch (IllegalArgumentException e) {
            // a broken key or url in the settings. its message could hold the key, so it stays out of the log
            log.warn("Groq request could not be built: check GROQ_API_KEY and groq.base-url");
            return "";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "";
        }
    }

    // our instructions and the user's text go as two separate messages, so the text is never part of the rules
    private HttpRequest buildRequest(String instructions, String text) throws IOException {
        Map<String, Object> body = new HashMap<>();
        body.put("model", groq.model());
        body.put("temperature", 0.2);
        // a thinking model (gpt-oss) spends tokens on thinking first, a small limit gives an empty answer
        body.put("max_tokens", 1000);
        // an empty setting = a normal model, nothing sent
        Optional.ofNullable(groq.reasoningEffort())
                .filter(effort -> !effort.isBlank())
                .ifPresent(effort -> body.put("reasoning_effort", effort.trim()));
        body.put("messages", List.of(
                Map.of("role", "system", "content", instructions),
                Map.of("role", "user", "content", text)));
        return HttpRequest.newBuilder()
                .uri(URI.create(groq.baseUrl() + "/chat/completions"))
                .timeout(Duration.ofSeconds(groq.timeoutSeconds()))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + groq.apiKey().trim())
                // groq can refuse java's default User-Agent
                .header("User-Agent", "gridpulse")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();
    }

    private String refused(int status) {
        log.warn("Groq answered HTTP {}", status);
        return "";
    }

    private String readAnswer(String json) throws IOException {
        String answer = objectMapper.readTree(json)
                .path("choices").path(0).path("message").path("content")
                .asText("").trim();
        // 200 but no text: a thinking model ran out of tokens
        Optional.of(answer)
                .filter(String::isEmpty)
                .ifPresent(empty -> log.warn("Groq sent an empty answer"));
        return answer;
    }
}
