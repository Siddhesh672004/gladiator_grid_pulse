package com.examly.springapp.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

// rules for the 6-digit codes (gridpulse.otp.*)
@ConfigurationProperties(prefix = "gridpulse.otp")
public record OtpProperties(
        int signupMinutes,
        int resetMinutes,
        int maxAttempts,      // wrong tries before the code is dead
        int resendSeconds,    // a new code at most once in this time
        int inviteHours) {    // how long the set-password link of a new admin works
}
