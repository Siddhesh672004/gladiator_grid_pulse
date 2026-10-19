package com.gridpulse.core.config;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.gridpulse.core.config.properties.CloudinaryProperties;
import com.gridpulse.core.config.properties.CorsProperties;
import com.gridpulse.core.config.properties.InternalApiProperties;
import com.gridpulse.core.config.properties.JwtProperties;
import com.gridpulse.core.config.properties.SuperAdminProperties;
import com.gridpulse.core.config.properties.TicketProperties;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// prod profile only: refuses to start when a must-have setting is missing
@Slf4j
@Configuration
@Profile("prod")
@RequiredArgsConstructor
public class ProdSettingsCheck {

    private final JwtProperties jwtProperties;
    private final CorsProperties corsProperties;
    private final SuperAdminProperties superAdminProperties;
    private final CloudinaryProperties cloudinaryProperties;
    private final TicketProperties ticketProperties;
    private final InternalApiProperties internalApiProperties;

    @PostConstruct
    public void check() {
        // first setting that is missing stops the start
        missingSettings().entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .findFirst()
                .ifPresent(this::fail);
        log.info("prod settings checked: jwt, cors, ticket secret, super admin, cloudinary and internal key are set");
    }

    // what to set -> is it missing, kept in the order they are reported
    private Map<String, Boolean> missingSettings() {
        Map<String, Boolean> missing = new LinkedHashMap<>();
        missing.put("set JWT_SECRET (at least 32 characters)",
                isMissing(jwtProperties.secret()) || jwtProperties.secret().length() < 32);
        missing.put("set CORS_ALLOWED_ORIGINS to your website, e.g. https://gridpulse.example.com", corsMissing());
        // the e-ticket codes must not be made with the known dev secret
        missing.put("set TICKET_SECRET (at least 32 characters)",
                isMissing(ticketProperties.ticketSecret()) || ticketProperties.ticketSecret().length() < 32);
        // the other services call /internal/** with it, the known dev key must not open it
        missing.put("set INTERNAL_API_KEY (at least 32 characters)",
                isMissing(internalApiProperties.internalApiKey())
                        || internalApiProperties.internalApiKey().length() < 32);
        missing.put("set SUPER_ADMIN_EMAIL and SUPER_ADMIN_PASSWORD",
                isMissing(superAdminProperties.email()) || isMissing(superAdminProperties.password()));
        // no cloudinary = no track can be added (pictures are required)
        missing.put("set CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY and CLOUDINARY_API_SECRET",
                !cloudinaryProperties.isConfigured());
        return missing;
    }

    private boolean corsMissing() {
        // behind the gateway the core answers no CORS: nothing to set here
        // no list at all, an empty entry or "*" (= every website) is not ok in prod
        return corsProperties.enabled() && Optional.ofNullable(corsProperties.allowedOrigins())
                .map(origins -> origins.stream().anyMatch(origin -> isMissing(origin) || origin.equals("*")))
                .orElse(true);
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
