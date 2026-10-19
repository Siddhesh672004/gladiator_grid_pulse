package com.gridpulse.ai.feedback.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.gridpulse.ai.feedback.dto.AiLinesResponse;
import com.gridpulse.ai.feedback.dto.FeedbackLabel;
import com.gridpulse.ai.feedback.dto.LabelRequest;
import com.gridpulse.ai.feedback.dto.SuggestionRequest;
import com.gridpulse.ai.feedback.service.FeedbackAiService;
import com.gridpulse.web.security.AdminOnly;
import com.gridpulse.web.security.UserOnly;

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
