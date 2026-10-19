package com.gridpulse.core.stats.data;

// one driver on the leaderboard (completed sessions only)
public interface LeaderboardRow {

    String getUsername();

    long getWins();

    long getPodiums();

    long getLaps();
}
