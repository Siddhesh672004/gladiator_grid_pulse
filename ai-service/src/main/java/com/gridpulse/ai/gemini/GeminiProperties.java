package com.gridpulse.ai.gemini;

import java.util.List;
import java.util.stream.Stream;

import org.springframework.boot.context.properties.ConfigurationProperties;

// the gemini.* lines of application.properties (keys from GEMINI_API_KEY, GEMINI_API_KEY_2, GEMINI_API_KEY_3)
@ConfigurationProperties(prefix = "gemini")
public record GeminiProperties(
        String apiKey,
        String apiKey2,
        String apiKey3,
        String embeddingModel,
        String generationModel,
        String baseUrl,
        int timeoutSeconds) {

    // only the keys that are filled in, in the order they are used
    public List<String> keys() {
        return Stream.of(apiKey, apiKey2, apiKey3)
                .filter(key -> key != null && !key.isBlank())
                .map(String::trim)
                .toList();
    }

    // the keys never go into a log line, only how many are filled in
    @Override
    public String toString() {
        return "GeminiProperties[keys=" + keys().size() + " set, embeddingModel=" + embeddingModel
                + ", generationModel=" + generationModel + ", baseUrl=" + baseUrl + ", timeoutSeconds="
                + timeoutSeconds + "]";
    }
}
