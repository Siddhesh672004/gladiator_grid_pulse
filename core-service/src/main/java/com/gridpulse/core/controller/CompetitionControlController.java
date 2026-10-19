package com.gridpulse.core.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gridpulse.core.dto.response.CompetitionResponse;
import com.gridpulse.core.dto.response.LiveStandingResponse;
import com.gridpulse.core.service.CompetitionControlService;
import com.gridpulse.web.security.AdminOnly;

import lombok.RequiredArgsConstructor;

// buttons of the Competition Control page; pressed at the wrong moment -> 400
@RestController
@RequestMapping("/api/competition-control")
@RequiredArgsConstructor
public class CompetitionControlController {

    private final CompetitionControlService competitionControlService;

    @AdminOnly
    @PostMapping("/{competitionId}/qualifying/start")
    public ResponseEntity<CompetitionResponse> startQualifying(@PathVariable Long competitionId) {
        return ResponseEntity.ok(competitionControlService.startQualifying(competitionId));
    }

    @AdminOnly
    @PostMapping("/{competitionId}/qualifying/finish")
    public ResponseEntity<CompetitionResponse> finishQualifying(@PathVariable Long competitionId) {
        return ResponseEntity.ok(competitionControlService.finishQualifying(competitionId));
    }

    @AdminOnly
    @PostMapping("/{competitionId}/race/start")
    public ResponseEntity<CompetitionResponse> startRace(@PathVariable Long competitionId) {
        return ResponseEntity.ok(competitionControlService.startRace(competitionId));
    }

    @AdminOnly
    @PostMapping("/{competitionId}/race/finish")
    public ResponseEntity<CompetitionResponse> finishRace(@PathVariable Long competitionId) {
        return ResponseEntity.ok(competitionControlService.finishRace(competitionId));
    }

    @AdminOnly
    @PostMapping("/lap/{raceBookingId}")
    public ResponseEntity<LiveStandingResponse> addLap(@PathVariable Long raceBookingId) {
        return ResponseEntity.ok(competitionControlService.addLap(raceBookingId));
    }

    @AdminOnly
    @PostMapping("/retire/{raceBookingId}")
    public ResponseEntity<LiveStandingResponse> retireCar(@PathVariable Long raceBookingId) {
        return ResponseEntity.ok(competitionControlService.retireCar(raceBookingId));
    }
}
