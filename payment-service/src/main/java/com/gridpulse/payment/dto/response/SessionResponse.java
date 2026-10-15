package com.gridpulse.payment.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;

// the session as the core's SessionResponse shows it, from the payment's copy; what is not copied stays null
public record SessionResponse(
        Long sessionId,
        Long trackId,
        String trackName,
        String trackLocation,
        String sessionType,
        String phase,
        Long competitionId,
        String competitionName,
        LocalDate sessionDate,
        LocalTime startTime,
        LocalTime endTime,
        Double price,
        Integer maxCars,
        String status,
        Long approvedCars) {
}
