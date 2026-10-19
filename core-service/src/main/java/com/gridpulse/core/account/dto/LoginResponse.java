package com.gridpulse.core.account.dto;

import com.gridpulse.core.account.data.Role;

public record LoginResponse(String token, String username, Role userRole, int userId) {
}
