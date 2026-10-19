package com.gridpulse.core.dto.response;

import java.time.LocalDate;

import com.gridpulse.core.model.CompetitionStatus;

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
