package com.gridpulse.ai.dto.response;

import java.time.Instant;

// one Confirm card under the Copilot's reply
public record ActionCard(String token, String summary, Instant expiresAt) {
}
