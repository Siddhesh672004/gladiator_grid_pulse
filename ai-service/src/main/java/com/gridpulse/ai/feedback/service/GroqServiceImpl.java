package com.gridpulse.ai.feedback.service;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Optional;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;

// groq speaks the openai api: spring ai's openai client with groq's url and key. no key = no client, the callers
// get their fallbacks. only the kind of failure is logged, never the key or the feedback text
@Slf4j
@Service
public class GroqServiceImpl implements GroqService {

    private final Optional<ChatClient> groq;

    public GroqServiceImpl(GroqProperties properties) {
        this.groq = Optional.of(properties)
                .filter(GroqProperties::hasKey)
                .map(GroqServiceImpl::chatClient);
        groq.ifPresentOrElse(client -> log.info("Groq enabled (model: {})", properties.model()),
                () -> log.info("GROQ_API_KEY not set - feedback is saved without an AI label"));
    }

    @Override
    public boolean isEnabled() {
        return groq.isPresent();
    }

    @Override
    @CircuitBreaker(name = "groq", fallbackMethod = "noAnswer")
    public String ask(String instructions, String text) {
        // no key -> "" without a call
        return groq.map(client -> Optional.ofNullable(client.prompt().system(instructions).user(text).call().content())
                        .map(String::trim)
                        .orElse(""))
                .orElse("");
    }

    @Override
    @CircuitBreaker(name = "groq", fallbackMethod = "noEntity")
    public <T> T askFor(String instructions, String text, Class<T> type) {
        return groq.map(client -> client.prompt().system(instructions).user(text).call().entity(type))
                .orElse(null);
    }

    // groq failed, or the breaker is open after many failures
    private String noAnswer(String instructions, String text, Exception e) {
        log.warn("Groq gave no answer ({})", e.getClass().getSimpleName());
        return "";
    }

    private <T> T noEntity(String instructions, String text, Class<T> type, Exception e) {
        log.warn("Groq gave no usable {} ({})", type.getSimpleName(), e.getClass().getSimpleName());
        return null;
    }

    private static ChatClient chatClient(GroqProperties properties) {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(properties.timeoutSeconds())).build());
        factory.setReadTimeout(Duration.ofSeconds(properties.timeoutSeconds()));
        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(properties.baseUrl())
                .apiKey(properties.apiKey().trim())
                .restClientBuilder(RestClient.builder().requestFactory(factory))
                .build();
        OpenAiChatOptions.Builder options = OpenAiChatOptions.builder()
                .model(properties.model())
                .temperature(0.2)
                // a thinking model (gpt-oss) spends tokens on thinking first, a small limit gives an empty answer
                .maxTokens(1000);
        // an empty setting = a normal model, nothing sent
        Optional.ofNullable(properties.reasoningEffort())
                .filter(effort -> !effort.isBlank())
                .ifPresent(effort -> options.reasoningEffort(effort.trim()));
        // one try: the circuit breaker and the fallbacks decide what happens next
        return ChatClient.create(OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(options.build())
                .retryTemplate(RetryTemplate.builder().maxAttempts(1).build())
                .build());
    }
}
