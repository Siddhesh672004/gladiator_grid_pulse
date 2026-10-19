package com.gridpulse.core.platform.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

// the jwt.* lines of application.properties, used by JwtUtils
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret,
        String secretFile,
        long expirationMs) {    // 36000000 = 10 hours
}
