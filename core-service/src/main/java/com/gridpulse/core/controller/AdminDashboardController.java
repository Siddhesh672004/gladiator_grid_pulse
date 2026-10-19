package com.gridpulse.core.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gridpulse.core.dto.response.AdminDashboardResponse;
import com.gridpulse.core.dto.response.LiveRaceResponse;
import com.gridpulse.core.security.annotation.AdminOnly;
import com.gridpulse.core.service.AdminDashboardService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @AdminOnly
    @GetMapping
    public ResponseEntity<AdminDashboardResponse> getDashboard() {
        return ResponseEntity.ok(adminDashboardService.getDashboard());
    }

    // the page polls this every 4 seconds
    @AdminOnly
    @GetMapping("/live-races")
    public ResponseEntity<List<LiveRaceResponse>> getLiveRaces() {
        return ResponseEntity.ok(adminDashboardService.getLiveRaces());
    }
}
