package com.gridpulse.payment.client;

import java.time.LocalDate;
import java.time.LocalTime;

// the core's answer to /internal/bookings/check: every booking rule passed, the price + what the payment keeps
public record BookingQuote(
        Long sessionId,
        Double price,
        String description,
        boolean waitlisted,
        int userId,
        String driverName,
        String driverEmail,
        String driverMobile,
        String trackName,
        String trackLocation,
        LocalDate sessionDate,
        LocalTime startTime,
        LocalTime endTime,
        String sessionType,
        String phase,
        String competitionName) {
}
