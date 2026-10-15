package com.gridpulse.ai.dto.request;

import java.util.Optional;

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
        // only spaces = nothing typed
        message = Optional.ofNullable(message)
                .filter(typed -> !typed.isBlank())
                .map(String::trim)
                .orElse(null);
    }
}
