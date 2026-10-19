package com.gridpulse.core.competition.dto;

import java.time.LocalDate;

import com.gridpulse.core.competition.data.CompetitionStatus;
import com.gridpulse.core.session.dto.SessionResponse;

public record CompetitionResponse(
        Long competitionId,
        String competitionName,
        Long trackId,
        String trackName,
        String trackCategory,
        LocalDate competitionDate,
        CompetitionStatus status,
        Integer qualifyingLaps,
        Integer raceLaps,
        Integer maxCars,
        SessionResponse qualifyingSession,
        SessionResponse raceSession) {
}
