package com.gridpulse.payment.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

// jwt.secret (JWT_SECRET, the core's) and the core's key file for a laptop without it (config/JwtConfig)
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(String secret, String secretFile) {

    // the secret never goes into a log line
    @Override
    public String toString() {
        return "JwtProperties[secret=***, secretFile=" + secretFile + "]";
    }
}
