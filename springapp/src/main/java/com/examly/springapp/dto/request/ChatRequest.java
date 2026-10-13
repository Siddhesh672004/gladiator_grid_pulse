package com.examly.springapp.dto.request;

import com.examly.springapp.validation.ValidationRules;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChatRequest(

        @NotBlank(message = "Message is required")
        @Size(max = 1000, message = "Message must be at most 1000 characters")
        String message,

        // empty on the first message, the server makes one (a uuid) and the chat window sends it back
        @Pattern(regexp = "^([0-9a-f-]{36})?$", message = "Chat session id is invalid")
        String sessionId) {

    // counted without the spaces around it, like every other text box
    public ChatRequest {
        message = ValidationRules.clean(message);
    }
}
