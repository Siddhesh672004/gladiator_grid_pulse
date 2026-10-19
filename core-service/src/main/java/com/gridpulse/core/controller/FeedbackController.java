package com.gridpulse.core.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gridpulse.common.paging.PageResponse;
import com.gridpulse.core.dto.request.FeedbackRequest;
import com.gridpulse.core.dto.response.FeedbackCountsResponse;
import com.gridpulse.core.dto.response.FeedbackResponse;
import com.gridpulse.core.service.FeedbackService;
import com.gridpulse.web.security.AdminOnly;
import com.gridpulse.web.security.LoggedIn;
import com.gridpulse.web.security.UserOnly;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    // the author comes from the token; 409 = second feedback for the same session
    @UserOnly
    @PostMapping
    public ResponseEntity<FeedbackResponse> createFeedback(@Valid @RequestBody FeedbackRequest request) {
        return ResponseEntity.status(201).body(feedbackService.createFeedback(request));
    }

    @AdminOnly
    @PostMapping("/{id}/analyse")
    public ResponseEntity<FeedbackResponse> analyseFeedback(@PathVariable Long id) {
        return ResponseEntity.ok(feedbackService.analyseFeedback(id));
    }

    @LoggedIn
    @GetMapping("/{id}")
    public ResponseEntity<FeedbackResponse> getFeedbackById(@PathVariable Long id) {
        return ResponseEntity.ok(feedbackService.getFeedbackById(id));
    }

    // admin only: rows carry the author's email, mobile and the ai label
    @AdminOnly
    @GetMapping
    public ResponseEntity<List<FeedbackResponse>> getAllFeedbacks() {
        return ResponseEntity.ok(feedbackService.getAllFeedbacks());
    }

    // one page of the admin list, page counts from 0
    @AdminOnly
    @GetMapping("/page")
    public ResponseEntity<PageResponse<FeedbackResponse>> getFeedbackPage(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sentiment,
            @RequestParam(required = false) String rating,
            @RequestParam(defaultValue = "false") boolean urgent,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(feedbackService.getFeedbackPage(search, sentiment, rating, urgent, page, size));
    }

    // the counters above the table: all feedback, not one page
    @AdminOnly
    @GetMapping("/counts")
    public ResponseEntity<FeedbackCountsResponse> getCounts() {
        return ResponseEntity.ok(feedbackService.getCounts());
    }

    @UserOnly
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<FeedbackResponse>> getFeedbacksByUserId(@PathVariable int userId) {
        return ResponseEntity.ok(feedbackService.getFeedbacksByUserId(userId));
    }

    @AdminOnly
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<List<FeedbackResponse>> getFeedbacksBySessionId(@PathVariable Long sessionId) {
        return ResponseEntity.ok(feedbackService.getFeedbacksBySessionId(sessionId));
    }

    @UserOnly
    @DeleteMapping("/{id}")
    public ResponseEntity<FeedbackResponse> deleteFeedback(@PathVariable Long id) {
        return ResponseEntity.ok(feedbackService.deleteFeedback(id));
    }
}
