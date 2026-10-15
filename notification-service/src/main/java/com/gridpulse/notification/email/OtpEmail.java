package com.gridpulse.notification.email;

// sign up step 2: the 6-digit code that proves the email address
public record OtpEmail(String username, String code, int validMinutes) implements EmailMessage {

    @Override
    public String subject() {
        return "Your GridPulse verification code";
    }

    @Override
    public String body() {
        return """
                Hi %s,

                Welcome to GridPulse. To finish signing up, type this verification code: %s

                It works once and for %d minutes.
                If you did not just sign up for GridPulse, ignore this email - no account is made without the code.

                - GridPulse
                """.formatted(username, code, validMinutes);
    }

    @Override
    public String toString() {
        return "OtpEmail[username=" + username + "]";
    }
}
