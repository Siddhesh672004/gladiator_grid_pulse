package com.gridpulse.core.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gridpulse.core.dto.response.RecentFeedbackResponse;
import com.gridpulse.core.service.FeedbackService;
import com.gridpulse.web.security.AdminOnly;

import lombok.RequiredArgsConstructor;

// for ai-service only (X-Internal-Key + the admin's own token): the feedback its summary is made from
@RestController
@RequestMapping("/internal/feedback")
@RequiredArgsConstructor
public class InternalFeedbackController {

    private final FeedbackService feedbackService;

    // newest first, at most 50
    @AdminOnly
    @GetMapping("/recent")
    public ResponseEntity<List<RecentFeedbackResponse>> recent(@RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(feedbackService.getRecentFeedback(limit));
    }
}
