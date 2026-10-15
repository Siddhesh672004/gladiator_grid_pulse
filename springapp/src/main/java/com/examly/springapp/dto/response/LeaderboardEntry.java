package com.examly.springapp.dto.response;

// one line of the public leaderboard: usernames only, nothing else about the driver
public record LeaderboardEntry(int position, String username, long wins, long podiums, long laps) {
}
