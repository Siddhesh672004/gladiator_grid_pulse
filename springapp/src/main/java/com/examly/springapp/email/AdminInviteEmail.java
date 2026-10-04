package com.examly.springapp.email;

// sent when the super admin adds an admin: email + initial password
public record AdminInviteEmail(String username, String email, String initialPassword) implements EmailMessage {

    @Override
    public String subject() {
        return "You are now a GridPulse admin";
    }

    @Override
    public String body() {
        return """
                Hi %s,

                The GridPulse Super Admin has made you an admin.

                Login page : Admin Login (the "Admin? Login here" link on the login page)
                Email      : %s
                Password   : %s

                Please change this password soon with "Forgot password" on the login page.

                - GridPulse
                """.formatted(username, email, initialPassword);
    }

    @Override
    public String toString() {
        return "AdminInviteEmail[username=" + username + ", email=" + email + "]";
    }
}
