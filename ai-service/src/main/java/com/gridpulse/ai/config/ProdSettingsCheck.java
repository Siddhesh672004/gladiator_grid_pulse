package com.gridpulse.ai.config;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.gridpulse.ai.config.properties.InternalApiProperties;
import com.gridpulse.ai.config.properties.JwtProperties;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// prod profile only: the server refuses to start with a dev fallback still in use
// no ai keys is allowed: the chat then answers from the faq list, feedback stays "Not analysed"
@Slf4j
@Configuration
@Profile("prod")
@RequiredArgsConstructor
public class ProdSettingsCheck {

    private final JwtProperties jwtProperties;
    private final InternalApiProperties internalApiProperties;

    @PostConstruct
    public void check() {
        missingSettings().entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .findFirst()
                .ifPresent(this::fail);
        log.info("prod settings checked: jwt and internal key are set");
    }

    private Map<String, Boolean> missingSettings() {
        Map<String, Boolean> missing = new LinkedHashMap<>();
        // no key file on a server: the core's JWT_SECRET is the only way to read its tokens
        missing.put("set JWT_SECRET (the core's, at least 32 characters)",
                isMissing(jwtProperties.secret()) || jwtProperties.secret().length() < 32);
        missing.put("set INTERNAL_API_KEY (at least 32 characters, the same as the core's)",
                isMissing(internalApiProperties.internalApiKey())
                        || internalApiProperties.internalApiKey().length() < 32);
        return missing;
    }

    private boolean isMissing(String value) {
        // not set, blank or still a ${PLACEHOLDER} nobody filled in
        return Optional.ofNullable(value)
                .map(text -> text.isBlank() || text.contains("${"))
                .orElse(true);
    }

    private void fail(String what) {
        throw new IllegalStateException("prod profile: " + what);
    }
}
