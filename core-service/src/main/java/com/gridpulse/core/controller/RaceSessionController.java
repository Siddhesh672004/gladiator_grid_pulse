package com.gridpulse.core.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gridpulse.core.dto.request.SessionRequest;
import com.gridpulse.core.dto.response.PageResponse;
import com.gridpulse.core.dto.response.SessionResponse;
import com.gridpulse.core.security.annotation.AdminOnly;
import com.gridpulse.core.service.RaceSessionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// admin schedules practice sessions, everybody reads them (competition sessions: CompetitionController)
@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class RaceSessionController {

    private final RaceSessionService raceSessionService;

    // 400 bad slot, 409 overlaps another session
    @AdminOnly
    @PostMapping
    public ResponseEntity<SessionResponse> addPracticeSession(@Valid @RequestBody SessionRequest request) {
        return ResponseEntity.status(201).body(raceSessionService.addPracticeSession(request));
    }

    @AdminOnly
    @PutMapping("/{sessionId}")
    public ResponseEntity<SessionResponse> updatePracticeSession(@PathVariable Long sessionId,
            @Valid @RequestBody SessionRequest request) {
        return ResponseEntity.ok(raceSessionService.updatePracticeSession(sessionId, request));
    }

    // 400 once the session is no longer SCHEDULED
    @AdminOnly
    @PostMapping("/{sessionId}/cancel")
    public ResponseEntity<SessionResponse> cancelSession(@PathVariable Long sessionId) {
        return ResponseEntity.ok(raceSessionService.cancelSession(sessionId));
    }

    @AdminOnly
    @GetMapping
    public ResponseEntity<List<SessionResponse>> getAllSessions() {
        return ResponseEntity.ok(raceSessionService.getAllSessions());
    }

    // one page of the admin list, page counts from 0, type = Practice / Competition / All
    @AdminOnly
    @GetMapping("/page")
    public ResponseEntity<PageResponse<SessionResponse>> getSessionPage(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(raceSessionService.getSessionPage(search, status, type, page, size));
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<SessionResponse> getSessionById(@PathVariable Long sessionId) {
        return ResponseEntity.ok(raceSessionService.getSessionById(sessionId));
    }

    @GetMapping("/track/{trackId}")
    public ResponseEntity<List<SessionResponse>> getSessionsForTrackOnDate(@PathVariable Long trackId,
            @RequestParam LocalDate date) {
        return ResponseEntity.ok(raceSessionService.getSessionsForTrackOnDate(trackId, date));
    }
}
