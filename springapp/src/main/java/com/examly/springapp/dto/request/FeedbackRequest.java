package com.examly.springapp.dto.request;

import java.time.LocalDate;

import com.examly.springapp.validation.ValidationRules;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// the author is the logged-in user, the sentiment label comes from the ai and never from the driver
public record FeedbackRequest(

        @NotNull(message = "Session is required")
        Long sessionId,

        @NotNull(message = "Rating is required")
        @Min(value = 1, message = "Rating must be at least 1")
        @Max(value = 5, message = "Rating must be at most 5")
        Integer rating,

        @NotBlank(message = "Feedback is required")
        @Size(min = 10, message = "Feedback must be at least 10 characters")
        @Size(max = 1000, message = "Feedback must be at most 1000 characters")
        String feedbackText,

        // not in the future: FeedbackServiceImpl.checkDate (India's date, the server may run in UTC)
        LocalDate date) {

    public FeedbackRequest {
        feedbackText = ValidationRules.clean(feedbackText);
    }
}
