package com.gridpulse.core.dto.response;

import java.util.List;

import com.gridpulse.common.paging.ChartPoint;

// the driver's own numbers over completed sessions. best finishes are empty until the first race / qualifying
// lapsPerMonth = Jan to Dec of this year
public record DriverStatsResponse(
        long sessionsDriven,
        long totalLaps,
        long competitionsEntered,
        long wins,
        long podiums,
        long dnfs,
        Integer bestRaceFinish,
        Integer bestQualifying,
        List<ChartPoint> lapsPerMonth) {
}
