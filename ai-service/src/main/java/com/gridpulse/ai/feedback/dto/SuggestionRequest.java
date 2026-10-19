package com.gridpulse.ai.feedback.dto;

import java.util.Optional;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// "Improve with AI": same text rules as the core's feedback form
public record SuggestionRequest(

        @NotBlank(message = "Feedback is required")
        @Size(min = 10, message = "Feedback must be at least 10 characters")
        @Size(max = 1000, message = "Feedback must be at most 1000 characters")
        String feedbackText) {

    public SuggestionRequest {
        // only spaces = nothing typed
        feedbackText = Optional.ofNullable(feedbackText)
                .filter(typed -> !typed.isBlank())
                .map(String::trim)
                .orElse(null);
    }
}
