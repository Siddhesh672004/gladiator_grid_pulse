package com.gridpulse.gateway.ratelimit;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;

// the user id inside a core token (HS256 with JWT_SECRET, like common's JwtVerifier; the gateway can't use common)
// a bad, expired or missing token -> empty: that visitor is counted by address
@Slf4j
@Component
public class TokenUsers {

    private final Optional<byte[]> key;

    public TokenUsers(@Value("${gridpulse.jwt-secret:}") String secret) {
        this.key = Optional.ofNullable(secret)
                .filter(value -> !value.isBlank())
                .map(value -> value.getBytes(StandardCharsets.UTF_8));
        if (key.isEmpty()) {
            log.warn("JWT_SECRET is not set - the gateway counts every visitor by address, not by user");
        }
    }

    public Optional<Integer> userId(String authorization) {
        return key.flatMap(secret -> Optional.ofNullable(authorization)
                .filter(header -> header.startsWith("Bearer "))
                .map(header -> header.substring(7))
                .flatMap(token -> read(token, secret)));
    }

    private Optional<Integer> read(String token, byte[] secret) {
        try {
            return Optional.ofNullable(Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody()
                    .get("userId", Integer.class));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
