package com.examly.springapp.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

// the groq.* lines of application.properties (the ai for feedback, key from GROQ_API_KEY)
@ConfigurationProperties(prefix = "groq")
public record GroqProperties(
        String apiKey,
        String model,
        String baseUrl,
        int timeoutSeconds,
        String reasoningEffort) { // low / medium / high for "thinking" models, empty for the others

    public boolean hasKey() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public String toString() {
        return "GroqProperties[model=" + model + ", baseUrl=" + baseUrl + "]";
    }
}
