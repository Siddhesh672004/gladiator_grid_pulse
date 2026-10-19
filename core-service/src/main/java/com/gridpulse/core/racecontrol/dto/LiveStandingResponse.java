package com.gridpulse.core.racecontrol.dto;

import java.time.Instant;

import com.gridpulse.core.account.dto.UserSummaryResponse;

// one row of the PUBLIC live timing board: the driver is only a name, no license / price / comments
public record LiveStandingResponse(
        Long raceBookingId,
        UserSummaryResponse user,
        Integer carNumber,
        String carCategory,
        Integer lapCount,
        Instant lastLapAt,      // UTC, the board shows it in IST
        String raceState,
        Integer finalPosition,
        Integer gridPosition) { // starting place in a competition race, empty everywhere else
}
