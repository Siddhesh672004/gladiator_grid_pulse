package com.examly.springapp.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.examly.springapp.dto.request.FeedbackRequest;
import com.examly.springapp.dto.request.SuggestionRequest;
import com.examly.springapp.dto.response.AiLinesResponse;
import com.examly.springapp.dto.response.FeedbackResponse;
import com.examly.springapp.security.annotation.AdminOnly;
import com.examly.springapp.security.annotation.LoggedIn;
import com.examly.springapp.security.annotation.UserOnly;
import com.examly.springapp.service.FeedbackService;

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

    // "Improve with AI": nothing is saved, an empty list = the ai could not answer (still 200)
    @UserOnly
    @PostMapping("/suggestions")
    public ResponseEntity<AiLinesResponse> suggest(@Valid @RequestBody SuggestionRequest request) {
        return ResponseEntity.ok(feedbackService.suggest(request));
    }

    @AdminOnly
    @PostMapping("/{id}/analyse")
    public ResponseEntity<FeedbackResponse> analyseFeedback(@PathVariable Long id) {
        return ResponseEntity.ok(feedbackService.analyseFeedback(id));
    }

    @AdminOnly
    @GetMapping("/summary")
    public ResponseEntity<AiLinesResponse> summarise() {
        return ResponseEntity.ok(feedbackService.summarise());
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
