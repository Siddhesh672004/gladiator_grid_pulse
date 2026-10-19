package com.gridpulse.core.session.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import com.gridpulse.core.competition.data.CompetitionPhase;
import com.gridpulse.core.session.data.SessionStatus;
import com.gridpulse.core.session.data.SessionType;

// competitionId / competitionName / phase are empty for a practice session
// approvedCars = seats taken, empty when the session sits inside a booking
public record SessionResponse(
        Long sessionId,
        Long trackId,
        String trackName,
        String trackLocation,
        SessionType sessionType,
        CompetitionPhase phase,
        Long competitionId,
        String competitionName,
        LocalDate sessionDate,
        LocalTime startTime,
        LocalTime endTime,
        Double price,
        Integer maxCars,
        SessionStatus status,
        Long approvedCars) {
}
