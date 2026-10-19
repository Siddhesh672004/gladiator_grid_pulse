package com.gridpulse.core.stats.web;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gridpulse.core.stats.dto.DriverStatsResponse;
import com.gridpulse.core.stats.dto.LeaderboardEntry;
import com.gridpulse.core.stats.service.StatsService;
import com.gridpulse.web.security.UserOnly;

import lombok.RequiredArgsConstructor;

// driver stats (own numbers) + the public leaderboard of the start page
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @UserOnly
    @GetMapping("/me")
    public ResponseEntity<DriverStatsResponse> getMyStats() {
        return ResponseEntity.ok(statsService.getMyStats());
    }

    // public, usernames only. limit 1 - 20, anything else -> 400
    @GetMapping("/leaderboard")
    public ResponseEntity<List<LeaderboardEntry>> getLeaderboard(@RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(statsService.getLeaderboard(limit));
    }
}
