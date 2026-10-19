package com.gridpulse.ai.faq.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

// how sure the chatbot must be before it answers with an faq (chatbot.*)
@ConfigurationProperties(prefix = "chatbot")
public record ChatbotProperties(
        double similarityThreshold,
        double lexicalThreshold) {
}
