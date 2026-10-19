package com.gridpulse.core.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gridpulse.core.dto.CompetitionRule;
import com.gridpulse.core.dto.request.CompetitionRequest;
import com.gridpulse.core.dto.response.CompetitionResponse;
import com.gridpulse.core.security.annotation.AdminOnly;
import com.gridpulse.core.service.CompetitionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// admin creates / cancels competitions, everybody reads them (running one: CompetitionControlController)
@RestController
@RequestMapping("/api/competitions")
@RequiredArgsConstructor
public class CompetitionController {

    private final CompetitionService competitionService;

    // 201 with both sessions; 409 when a slot overlaps another session
    @AdminOnly
    @PostMapping
    public ResponseEntity<CompetitionResponse> createCompetition(@Valid @RequestBody CompetitionRequest request) {
        return ResponseEntity.status(201).body(competitionService.createCompetition(request));
    }

    @GetMapping
    public ResponseEntity<List<CompetitionResponse>> getAllCompetitions() {
        return ResponseEntity.ok(competitionService.getAllCompetitions());
    }

    @GetMapping("/{competitionId}")
    public ResponseEntity<CompetitionResponse> getCompetitionById(@PathVariable Long competitionId) {
        return ResponseEntity.ok(competitionService.getCompetitionById(competitionId));
    }

    // 400 once it is no longer UPCOMING
    @AdminOnly
    @PostMapping("/{competitionId}/cancel")
    public ResponseEntity<CompetitionResponse> cancelCompetition(@PathVariable Long competitionId) {
        return ResponseEntity.ok(competitionService.cancelCompetition(competitionId));
    }

    @AdminOnly
    @GetMapping("/rule/{trackId}")
    public ResponseEntity<CompetitionRule> getRuleForTrack(@PathVariable Long trackId) {
        return ResponseEntity.ok(competitionService.getRuleForTrack(trackId));
    }
}
