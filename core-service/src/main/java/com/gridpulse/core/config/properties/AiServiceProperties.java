package com.gridpulse.core.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

// gridpulse.ai-url (AI_URL): where ai-service runs, for service/AiClient
@ConfigurationProperties(prefix = "gridpulse")
public record AiServiceProperties(String aiUrl) {
}
