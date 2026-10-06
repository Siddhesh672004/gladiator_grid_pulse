package com.examly.springapp.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.examly.springapp.dto.request.TrackRequest;
import com.examly.springapp.dto.response.TrackResponse;
import com.examly.springapp.security.annotation.AdminOnly;
import com.examly.springapp.service.RaceTrackService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// admin adds / edits / deactivates tracks, everybody reads them
@RestController
@RequestMapping("/api/race-track")
@RequiredArgsConstructor
public class RaceTrackController {

    private final RaceTrackService raceTrackService;

    @AdminOnly
    @PostMapping
    public ResponseEntity<TrackResponse> addTrack(@Valid @RequestBody TrackRequest request) {
        return ResponseEntity.status(201).body(raceTrackService.addTrack(request));
    }

    @GetMapping("/{trackId}")
    public ResponseEntity<TrackResponse> getTrackById(@PathVariable Long trackId) {
        return ResponseEntity.ok(raceTrackService.getTrackById(trackId));
    }

    @GetMapping
    public ResponseEntity<List<TrackResponse>> getAllTracks() {
        return ResponseEntity.ok(raceTrackService.getAllTracks());
    }

    @AdminOnly
    @PutMapping("/{trackId}")
    public ResponseEntity<TrackResponse> updateTrack(@PathVariable Long trackId,
            @Valid @RequestBody TrackRequest request) {
        return ResponseEntity.ok(raceTrackService.updateTrack(trackId, request));
    }

    // 409 when the track ever had a session (deactivate it instead)
    @AdminOnly
    @DeleteMapping("/{trackId}")
    public ResponseEntity<TrackResponse> deleteTrack(@PathVariable Long trackId) {
        return ResponseEntity.ok(raceTrackService.deleteTrack(trackId));
    }

    // 409 while it still has upcoming sessions
    @AdminOnly
    @PostMapping("/{trackId}/deactivate")
    public ResponseEntity<TrackResponse> deactivateTrack(@PathVariable Long trackId) {
        return ResponseEntity.ok(raceTrackService.deactivateTrack(trackId));
    }

    @AdminOnly
    @PostMapping("/{trackId}/activate")
    public ResponseEntity<TrackResponse> activateTrack(@PathVariable Long trackId) {
        return ResponseEntity.ok(raceTrackService.activateTrack(trackId));
    }
}
