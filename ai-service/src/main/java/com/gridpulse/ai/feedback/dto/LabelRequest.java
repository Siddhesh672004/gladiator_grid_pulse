package com.gridpulse.ai.feedback.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// /internal/ai/label: the feedback text the core wants labelled
public record LabelRequest(

        @NotBlank(message = "Text is required")
        @Size(max = 2000, message = "Text must be at most 2000 characters")
        String text) {
}
