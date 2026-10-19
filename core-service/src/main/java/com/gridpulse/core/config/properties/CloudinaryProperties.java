package com.gridpulse.core.config.properties;

import java.util.Optional;

import org.springframework.boot.context.properties.ConfigurationProperties;

// the cloudinary.* lines of application.properties (keys come from CLOUDINARY_* in secrets.properties)
@ConfigurationProperties(prefix = "cloudinary")
public record CloudinaryProperties(
        String cloudName,
        String apiKey,
        String apiSecret,
        String folder,
        int timeoutSeconds) {

    public CloudinaryProperties {
        cloudName = trim(cloudName);
        apiKey = trim(apiKey);
        apiSecret = trim(apiSecret);
    }

    public boolean isConfigured() {
        return !cloudName.isEmpty() && !apiKey.isEmpty() && !apiSecret.isEmpty();
    }

    private static String trim(String value) {
        // a key that is not set counts as empty
        return Optional.ofNullable(value)
                .map(String::trim)
                .orElse("");
    }

    @Override
    public String toString() {
        return "CloudinaryProperties[cloudName=" + cloudName + ", folder=" + folder + "]";
    }
}
