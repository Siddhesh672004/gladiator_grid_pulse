package com.gridpulse.core.stats.dto;

import java.time.Instant;

public record RecentRegistrationResponse(
        Long raceBookingId,
        String username,
        String email,
        Instant registeredAt,
        String status,
        String userRole,
        String trackName,
        String session,
        String raceStatus) {
}
