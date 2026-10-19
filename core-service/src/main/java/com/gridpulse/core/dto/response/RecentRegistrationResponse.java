package com.gridpulse.core.dto.response;

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
