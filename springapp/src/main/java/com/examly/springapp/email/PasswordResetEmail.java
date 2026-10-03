package com.examly.springapp.email;

// forgot password: the 6-digit reset code
public record PasswordResetEmail(String username, String code, int validMinutes) implements EmailMessage {

    @Override
    public String subject() {
        return "Reset your GridPulse password";
    }

    @Override
    public String body() {
        return """
                Hi %s,

                Your GridPulse password reset code: %s

                Enter it on the Forgot Password page together with your new password.
                It works once and for %d minutes.
                If you did not ask for this, just ignore this email - your password stays the same.

                - GridPulse
                """.formatted(username, code, validMinutes);
    }

    @Override
    public String toString() {
        return "PasswordResetEmail[username=" + username + "]";
    }
}
