package com.gridpulse.core.dto.request;

// no @NotBlank on purpose: an empty email or password is just a wrong login (401), not a 400
public record LoginRequest(String email, String password) {

    // no password in log lines
    @Override
    public String toString() {
        return "LoginRequest[email=" + email + "]";
    }
}
