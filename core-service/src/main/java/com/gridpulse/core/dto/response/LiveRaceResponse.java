package com.gridpulse.core.dto.response;

import java.time.LocalTime;

// one row of the dashboard's Live Race Monitor (a session that is LIVE)
// totalLaps / progressPercentage only for a competition, a practice session has no lap limit
public record LiveRaceResponse(
        Long sessionId,
        String trackName,
        String session,
        LocalTime startTime,
        String raceStatus,
        int activeCars,
        int currentLap,              // the laps of the car in front
        Integer totalLaps,
        Integer progressPercentage) {
}
