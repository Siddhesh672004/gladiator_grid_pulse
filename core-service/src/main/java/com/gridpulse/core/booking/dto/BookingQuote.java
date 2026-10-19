package com.gridpulse.core.booking.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import com.gridpulse.core.competition.data.CompetitionPhase;
import com.gridpulse.core.session.data.SessionType;

// answer of /internal/bookings/check: every booking rule passed, what the payment needs to know
// waitlisted = the session is full, the booking would wait for a seat
// sessionType / phase / competitionName: so the admin payments page can still name the session
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
        SessionType sessionType,
        CompetitionPhase phase,
        String competitionName) {
}
