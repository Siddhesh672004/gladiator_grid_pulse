package com.gridpulse.core.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gridpulse.core.dto.response.LiveStandingResponse;
import com.gridpulse.core.service.RaceControlService;
import com.gridpulse.web.security.AdminOnly;

import lombok.RequiredArgsConstructor;

// live board of any session (public GET) + the buttons for PRACTICE sessions; competitions: CompetitionControlController
@RestController
@RequestMapping("/api/race-control")
@RequiredArgsConstructor
public class RaceControlController {

    private final RaceControlService raceControlService;

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<List<LiveStandingResponse>> getLiveStandings(@PathVariable Long sessionId) {
        return ResponseEntity.ok(raceControlService.getLiveStandings(sessionId));
    }

    @AdminOnly
    @PostMapping("/{raceBookingId}/start")
    public ResponseEntity<LiveStandingResponse> startRace(@PathVariable Long raceBookingId) {
        return ResponseEntity.ok(raceControlService.startRace(raceBookingId));
    }

    @AdminOnly
    @PostMapping("/{raceBookingId}/lap")
    public ResponseEntity<LiveStandingResponse> incrementLap(@PathVariable Long raceBookingId) {
        return ResponseEntity.ok(raceControlService.incrementLap(raceBookingId));
    }

    @AdminOnly
    @PostMapping("/{raceBookingId}/retire")
    public ResponseEntity<LiveStandingResponse> retireCar(@PathVariable Long raceBookingId) {
        return ResponseEntity.ok(raceControlService.retireCar(raceBookingId));
    }

    @AdminOnly
    @PostMapping("/session/{sessionId}/reset")
    public ResponseEntity<List<LiveStandingResponse>> resetSession(@PathVariable Long sessionId) {
        return ResponseEntity.ok(raceControlService.resetSession(sessionId));
    }

    @AdminOnly
    @PostMapping("/session/{sessionId}/finish")
    public ResponseEntity<List<LiveStandingResponse>> finishSession(@PathVariable Long sessionId) {
        return ResponseEntity.ok(raceControlService.finishSession(sessionId));
    }
}
