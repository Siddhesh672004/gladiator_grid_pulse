package com.gridpulse.ai.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

// how sure the chatbot must be before it answers with an faq (chatbot.*)
@ConfigurationProperties(prefix = "chatbot")
public record ChatbotProperties(
        double similarityThreshold,
        double lexicalThreshold) {
}
