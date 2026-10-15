package com.examly.springapp.service;

import java.util.List;

import com.examly.springapp.dto.response.DriverStatsResponse;
import com.examly.springapp.dto.response.LeaderboardEntry;

// numbers of completed sessions only: a scheduled or live session does not count yet
public interface StatsService {

    // the logged-in driver
    DriverStatsResponse getMyStats();

    // public, 1 - 20 drivers, wins first, then podiums, then laps
    List<LeaderboardEntry> getLeaderboard(int limit);
}
