package com.examly.springapp.dto.request;

import com.examly.springapp.validation.ValidationRules;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// "Improve with AI": same text rules as FeedbackRequest
public record SuggestionRequest(

        @NotBlank(message = "Feedback is required")
        @Size(min = 10, message = "Feedback must be at least 10 characters")
        @Size(max = 1000, message = "Feedback must be at most 1000 characters")
        String feedbackText) {

    public SuggestionRequest {
        feedbackText = ValidationRules.clean(feedbackText);
    }
}
