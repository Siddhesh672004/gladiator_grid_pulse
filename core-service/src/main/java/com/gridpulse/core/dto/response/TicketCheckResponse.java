package com.gridpulse.core.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;

// the gate's answer for a scanned code. not our code -> valid false and only the message
public record TicketCheckResponse(
        boolean valid,
        Long bookingId,
        String driverName,
        Integer carNumber,
        String trackName,
        LocalDate sessionDate,
        LocalTime startTime,
        String bookingStatus,
        String message) {
}
