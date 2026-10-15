package com.gridpulse.ai.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.gridpulse.ai.config.properties.JwtProperties;
import com.gridpulse.common.security.JwtVerifier;

import lombok.extern.slf4j.Slf4j;

// checks the core's tokens: with JWT_SECRET, or on a laptop without it with the key file the core made
@Slf4j
@Configuration
public class JwtConfig {

    @Bean
    public JwtVerifier jwtVerifier(JwtProperties jwtProperties) {
        return Optional.ofNullable(jwtProperties.secret())
                .filter(secret -> !secret.isBlank())
                .map(JwtVerifier::new)
                .orElseGet(() -> fromKeyFile(Path.of(jwtProperties.secretFile())));
    }

    private JwtVerifier fromKeyFile(Path file) {
        try {
            byte[] key = Base64.getDecoder().decode(Files.readString(file).trim());
            log.warn("JWT_SECRET is not set - tokens are checked with the core's key file {} (this laptop only)", file);
            return new JwtVerifier(key);
        } catch (IOException | IllegalArgumentException e) {
            throw new IllegalStateException("JWT_SECRET is not set and the core's key file " + file
                    + " can't be read: start the core once first, or set JWT_SECRET", e);
        }
    }
}
