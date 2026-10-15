package com.examly.springapp.event;

// a sign up or reset code was made, purpose "SIGNUP" or "RESET_PASSWORD"
public record OtpRequested(String email, String username, String code, int minutes, String purpose) {

    // the code never goes into a log line
    @Override
    public String toString() {
        return "OtpRequested[email=" + email + ", purpose=" + purpose + "]";
    }
}
