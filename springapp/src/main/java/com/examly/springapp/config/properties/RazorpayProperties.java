package com.examly.springapp.config.properties;

import java.util.Optional;

import org.springframework.boot.context.properties.ConfigurationProperties;

// the razorpay.* lines of application.properties (keys come from RAZORPAY_* in secrets.properties)
@ConfigurationProperties(prefix = "razorpay")
public record RazorpayProperties(
        String keyId,
        String keySecret,
        String baseUrl,
        int timeoutSeconds) {

    public RazorpayProperties {
        // a key that is not set counts as empty
        keyId = Optional.ofNullable(keyId).map(String::trim).orElse("");
        keySecret = Optional.ofNullable(keySecret).map(String::trim).orElse("");
    }

    public boolean isConfigured() {
        return !keyId.isEmpty() && !keySecret.isEmpty();
    }

    @Override
    public String toString() {
        return "RazorpayProperties[keyId=" + keyId + ", baseUrl=" + baseUrl + "]";
    }
}
