package com.examly.springapp.event;

// the super admin added an admin, link = the set-password page with the one-time token
public record AdminInvited(String email, String username, String link, int hours) {

    // the link holds the token, it never goes into a log line
    @Override
    public String toString() {
        return "AdminInvited[email=" + email + "]";
    }
}
