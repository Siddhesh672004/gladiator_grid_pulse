package com.examly.springapp.dto.response;

import com.examly.springapp.model.Role;

public record LoginResponse(String token, String username, Role userRole, int userId) {
}
