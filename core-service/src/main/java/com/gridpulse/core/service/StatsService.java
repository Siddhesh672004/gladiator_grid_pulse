package com.gridpulse.core.service;

import java.util.List;

import com.gridpulse.core.dto.response.DriverStatsResponse;
import com.gridpulse.core.dto.response.LeaderboardEntry;

// numbers of completed sessions only: a scheduled or live session does not count yet
public interface StatsService {

    // the logged-in driver
    DriverStatsResponse getMyStats();

    // public, 1 - 20 drivers, wins first, then podiums, then laps
    List<LeaderboardEntry> getLeaderboard(int limit);
}
