package com.examly.springapp.email;

// sent when the super admin adds an admin (or sends the invite again): the link to choose a password
public record AdminInviteEmail(String username, String link, int validHours) implements EmailMessage {

    @Override
    public String subject() {
        return "You are now a GridPulse admin";
    }

    @Override
    public String body() {
        return """
                Hi %s,

                The GridPulse Super Admin has made you an admin.

                Choose your password here (the link works once and for %d hours):
                %s

                Then log in on the Admin Login page (the "Admin? Login here" link on the login page).
                If the link has expired, ask the Super Admin to send a new invite.

                - GridPulse
                """.formatted(username, validHours, link);
    }

    // the link holds the token, it never goes into a log line
    @Override
    public String toString() {
        return "AdminInviteEmail[username=" + username + "]";
    }
}
