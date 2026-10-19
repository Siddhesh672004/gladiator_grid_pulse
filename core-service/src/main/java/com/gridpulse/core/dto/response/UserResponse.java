package com.gridpulse.core.dto.response;

import com.gridpulse.core.model.Role;

// what the api sends about an account, no password field at all so it can never be sent by mistake
public record UserResponse(
        int userId,
        String email,
        String username,
        String mobileNumber,
        Role userRole) {
}
