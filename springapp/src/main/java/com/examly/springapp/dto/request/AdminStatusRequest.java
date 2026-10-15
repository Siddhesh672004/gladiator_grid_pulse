package com.examly.springapp.dto.request;

import jakarta.validation.constraints.NotNull;

public record AdminStatusRequest(

        @NotNull(message = "enabled is required")
        Boolean enabled) {
}
