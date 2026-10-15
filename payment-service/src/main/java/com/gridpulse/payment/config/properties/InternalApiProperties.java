package com.gridpulse.payment.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

// gridpulse.internal-api-key (INTERNAL_API_KEY): the key this service sends on the core's /internal/** calls
@ConfigurationProperties(prefix = "gridpulse")
public record InternalApiProperties(String internalApiKey) {

    // the key never goes into a log line
    @Override
    public String toString() {
        return "InternalApiProperties[internalApiKey=***]";
    }
}
