package com.examly.springapp.dto.request;

import com.examly.springapp.validation.StrongPassword;
import com.examly.springapp.validation.ValidationRules;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(

        @NotBlank(message = ValidationRules.EMAIL_REQUIRED)
        @Size(max = ValidationRules.EMAIL_MAX, message = ValidationRules.EMAIL_TOO_LONG)
        @Pattern(regexp = ValidationRules.EMAIL_NO_SPACE_PATTERN, message = ValidationRules.EMAIL_HAS_SPACE)
        @Pattern(regexp = ValidationRules.EMAIL_ONE_AT_PATTERN, message = ValidationRules.EMAIL_ONE_AT)
        @Pattern(regexp = ValidationRules.EMAIL_DOMAIN_PATTERN, message = ValidationRules.EMAIL_NO_DOMAIN)
        @Pattern(regexp = ValidationRules.EMAIL_PATTERN, message = ValidationRules.EMAIL_INVALID)
        String email,

        @NotBlank(message = ValidationRules.CODE_REQUIRED)
        @Pattern(regexp = ValidationRules.CODE_DIGITS_PATTERN, message = ValidationRules.CODE_DIGITS_ONLY)
        @Pattern(regexp = ValidationRules.CODE_LENGTH_PATTERN, message = ValidationRules.CODE_INVALID)
        String code,

        @NotBlank(message = "New password is required")
        @StrongPassword
        String newPassword) {

    // the new password stays as typed
    public ResetPasswordRequest {
        email = ValidationRules.clean(email);
        code = ValidationRules.clean(code);
    }

    // no code / password in log lines
    @Override
    public String toString() {
        return "ResetPasswordRequest[email=" + email + "]";
    }
}
