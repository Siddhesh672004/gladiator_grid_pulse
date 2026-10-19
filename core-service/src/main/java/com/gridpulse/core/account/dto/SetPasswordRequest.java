package com.gridpulse.core.account.dto;

import com.gridpulse.core.platform.validation.StrongPassword;
import com.gridpulse.core.platform.validation.ValidationRules;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// a new admin opens the invite link (email + token come from the link) and chooses a password
public record SetPasswordRequest(

        @NotBlank(message = ValidationRules.EMAIL_REQUIRED)
        @Size(max = ValidationRules.EMAIL_MAX, message = ValidationRules.EMAIL_TOO_LONG)
        String email,

        @NotBlank(message = "The invite link is not complete. Please open it again from the email.")
        @Size(max = 100, message = "The invite link is not valid.")
        String token,

        @NotBlank(message = "New password is required")
        @StrongPassword
        String newPassword) {

    // the password stays as typed
    public SetPasswordRequest {
        email = ValidationRules.clean(email);
        token = ValidationRules.clean(token);
    }

    // no token / password in log lines
    @Override
    public String toString() {
        return "SetPasswordRequest[email=" + email + "]";
    }
}
