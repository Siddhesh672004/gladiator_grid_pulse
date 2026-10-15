package com.gridpulse.common.security;

import java.time.Instant;

// what a core token says about its user (email = the subject)
public record JwtClaims(Integer userId, String username, String userRole, String email, Instant expiresAt) {
}
