package com.gridpulse.core.service;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.gridpulse.core.config.properties.AiServiceProperties;
import com.gridpulse.core.config.properties.InternalApiProperties;
import com.gridpulse.core.dto.response.FeedbackLabelResponse;
import com.gridpulse.common.security.InternalApi;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;

// ai-service's /internal/ai/label for the admin's "Analyse" button (new feedback is labelled over kafka)
// ai-service down or no answer -> NOT_ANALYSED, the feedback stays as it is
@Slf4j
@Component
public class AiClient {

    private static final FeedbackLabelResponse NOT_ANALYSED = new FeedbackLabelResponse("NOT_ANALYSED", false);

    private final RestClient restClient;

    public AiClient(AiServiceProperties properties, InternalApiProperties internal) {
        // groq gets 10 s, so a little more for the whole call
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build());
        factory.setReadTimeout(Duration.ofSeconds(15));
        this.restClient = RestClient.builder()
                .baseUrl(properties.aiUrl())
                .requestFactory(factory)
                .defaultHeader(InternalApi.HEADER, InternalApi.keyOrDevDefault(internal.internalApiKey()))
                .build();
    }

    @CircuitBreaker(name = "ai", fallbackMethod = "notAnalysed")
    public FeedbackLabelResponse label(String feedbackText) {
        return restClient.post().uri("/internal/ai/label").body(Map.of("text", feedbackText)).retrieve()
                .body(FeedbackLabelResponse.class);
    }

    private FeedbackLabelResponse notAnalysed(String feedbackText, Exception e) {
        log.warn("ai-service gave no label: {}", e.getClass().getSimpleName());
        return NOT_ANALYSED;
    }
}
