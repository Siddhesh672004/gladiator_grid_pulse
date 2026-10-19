package com.gridpulse.core.account.dto;

import com.gridpulse.core.account.data.Role;

// what the api sends about an account, no password field at all so it can never be sent by mistake
public record UserResponse(
        int userId,
        String email,
        String username,
        String mobileNumber,
        Role userRole) {
}
