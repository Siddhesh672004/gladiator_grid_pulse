package com.gridpulse.core.account.dto;

import jakarta.validation.constraints.NotNull;

public record AdminStatusRequest(

        @NotNull(message = "enabled is required")
        Boolean enabled) {
}
