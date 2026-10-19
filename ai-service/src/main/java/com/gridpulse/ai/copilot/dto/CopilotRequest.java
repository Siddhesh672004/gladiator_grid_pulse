package com.gridpulse.ai.copilot.dto;

import java.util.Optional;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CopilotRequest(

        @NotBlank(message = "Message is required")
        @Size(max = 1000, message = "Message must be at most 1000 characters")
        String message,

        // empty on the first message, the server makes one (a uuid) and the chat window sends it back
        @Pattern(regexp = "^([0-9a-f-]{36})?$", message = "Chat id is invalid")
        String chatId) {

    public CopilotRequest {
        // only spaces = nothing typed
        message = Optional.ofNullable(message)
                .filter(typed -> !typed.isBlank())
                .map(String::trim)
                .orElse(null);
    }
}
