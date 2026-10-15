package com.gridpulse.notification.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

// both values are for test runs only (MAIL_TO_FILE=true: nothing is sent, the texts go to fileDir)
@ConfigurationProperties(prefix = "gridpulse.mail")
public record EmailProperties(boolean toFile, String fileDir) {
}
