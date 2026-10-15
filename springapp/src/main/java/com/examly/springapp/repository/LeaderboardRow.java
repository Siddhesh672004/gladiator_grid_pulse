package com.examly.springapp.repository;

// one driver on the leaderboard (completed sessions only)
public interface LeaderboardRow {

    String getUsername();

    long getWins();

    long getPodiums();

    long getLaps();
}
