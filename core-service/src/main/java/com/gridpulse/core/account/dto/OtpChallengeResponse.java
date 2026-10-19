package com.gridpulse.core.account.dto;

// expiresInSeconds = how long the code works (0 = no new code was sent, the one from a moment ago is still the one)
public record OtpChallengeResponse(String email, String message, int expiresInSeconds) {
}
