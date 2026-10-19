package com.gridpulse.web.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

// gridpulse.internal-api-key (INTERNAL_API_KEY): the key the services send and check on /internal/** calls
@ConfigurationProperties(prefix = "gridpulse")
public record InternalApiProperties(String internalApiKey) {

    // the key never goes into a log line
    @Override
    public String toString() {
        return "InternalApiProperties[internalApiKey=***]";
    }
}
