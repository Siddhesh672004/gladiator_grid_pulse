package com.gridpulse.notification.config;

import java.util.Optional;

import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.gridpulse.notification.config.properties.EmailProperties;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// prod profile only: a real server always sends real emails
@Slf4j
@Configuration
@Profile("prod")
@RequiredArgsConstructor
public class ProdSettingsCheck {

    private final EmailProperties emailProperties;
    private final MailProperties mailProperties;

    @PostConstruct
    public void check() {
        boolean missing = emailProperties.toFile()
                || isMissing(mailProperties.getUsername()) || isMissing(mailProperties.getPassword());
        Optional.of(missing)
                .filter(Boolean::booleanValue)
                .ifPresent(problem -> {
                    throw new IllegalStateException("prod profile: set MAIL_USERNAME and MAIL_PASSWORD (gmail + app "
                            + "password) and leave MAIL_TO_FILE off - prod always sends real emails");
                });
        log.info("prod settings checked: mail is set");
    }

    private boolean isMissing(String value) {
        // not set, blank or still a ${PLACEHOLDER} nobody filled in
        return Optional.ofNullable(value)
                .map(text -> text.isBlank() || text.contains("${"))
                .orElse(true);
    }
}
