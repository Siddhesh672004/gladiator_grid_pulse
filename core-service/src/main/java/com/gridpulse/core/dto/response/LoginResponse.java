package com.gridpulse.core.dto.response;

import com.gridpulse.core.model.Role;

public record LoginResponse(String token, String username, Role userRole, int userId) {
}
