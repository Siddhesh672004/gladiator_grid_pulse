package com.gridpulse.core.dto.response;

import java.time.LocalDateTime;

public record AdminResponse(
        int userId,
        String email,
        String username,
        String mobileNumber,
        boolean enabled,
        LocalDateTime createdAt) {   // null for admins made before this column existed
}
