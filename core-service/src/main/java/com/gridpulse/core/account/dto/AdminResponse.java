package com.gridpulse.core.account.dto;

import java.time.LocalDateTime;

public record AdminResponse(
        int userId,
        String email,
        String username,
        String mobileNumber,
        boolean enabled,
        LocalDateTime createdAt) {   // null for admins made before this column existed
}
