package com.gridpulse.ai.security;

import java.util.Optional;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

// who is calling, read from the core's token (config/JwtTokenFilter); role = "User", "Admin" or "SuperAdmin"
public record TokenUser(int userId, String email, String username, String role) {

    public static TokenUser current() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(Authentication::getPrincipal)
                .filter(TokenUser.class::isInstance)
                .map(TokenUser.class::cast)
                .orElseThrow(() -> new AccessDeniedException("Please log in to continue"));
    }
}
