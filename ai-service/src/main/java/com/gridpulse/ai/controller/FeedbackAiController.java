package com.gridpulse.ai.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.gridpulse.ai.dto.FeedbackLabel;
import com.gridpulse.ai.dto.request.LabelRequest;
import com.gridpulse.ai.dto.request.SuggestionRequest;
import com.gridpulse.ai.dto.response.AiLinesResponse;
import com.gridpulse.ai.security.annotation.AdminOnly;
import com.gridpulse.ai.security.annotation.UserOnly;
import com.gridpulse.ai.service.FeedbackAiService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// the feedback ai: the two buttons of the feedback pages (same urls and json as when the core had them)
// and the label the core asks for on "Analyse"
@RestController
@RequiredArgsConstructor
public class FeedbackAiController {

    private final FeedbackAiService feedbackAiService;

    // "Improve with AI": nothing is saved, an empty list = the ai could not answer (still 200)
    @UserOnly
    @PostMapping("/api/feedback/suggestions")
    public ResponseEntity<AiLinesResponse> suggest(@Valid @RequestBody SuggestionRequest request) {
        return ResponseEntity.ok(new AiLinesResponse(feedbackAiService.suggest(request.feedbackText())));
    }

    @AdminOnly
    @GetMapping("/api/feedback/summary")
    public ResponseEntity<AiLinesResponse> summarise() {
        return ResponseEntity.ok(new AiLinesResponse(feedbackAiService.summariseRecent()));
    }

    // the core only (X-Internal-Key): the admin pressed "Analyse"
    @PostMapping("/internal/ai/label")
    public ResponseEntity<FeedbackLabel> label(@Valid @RequestBody LabelRequest request) {
        return ResponseEntity.ok(feedbackAiService.label(request.text()));
    }
}
