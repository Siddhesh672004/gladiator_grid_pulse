package com.gridpulse.core.account.dto;

import java.time.LocalDateTime;

public record AdminUserResponse(
        int userId,
        String username,
        String email,
        String mobileNumber,
        boolean enabled,
        boolean profileComplete,      // false = no date of birth / license yet, cannot book
        LocalDateTime createdAt) {
}
