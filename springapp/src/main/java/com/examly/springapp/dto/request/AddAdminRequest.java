package com.examly.springapp.dto.request;

import com.examly.springapp.validation.StrongPassword;
import com.examly.springapp.validation.ValidationRules;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// super admin adds an admin, the new admin gets email + initial password by mail
public record AddAdminRequest(

        @NotBlank(message = ValidationRules.USERNAME_REQUIRED)
        @Size(min = ValidationRules.USERNAME_MIN, message = ValidationRules.USERNAME_TOO_SHORT)
        @Size(max = ValidationRules.USERNAME_MAX, message = ValidationRules.USERNAME_TOO_LONG)
        @Pattern(regexp = ValidationRules.USERNAME_PATTERN, message = ValidationRules.USERNAME_INVALID)
        @Pattern(regexp = ValidationRules.USERNAME_ONE_SPACE_PATTERN, message = ValidationRules.USERNAME_DOUBLE_SPACE)
        String username,

        @NotBlank(message = ValidationRules.EMAIL_REQUIRED)
        @Size(max = ValidationRules.EMAIL_MAX, message = ValidationRules.EMAIL_TOO_LONG)
        @Pattern(regexp = ValidationRules.EMAIL_NO_SPACE_PATTERN, message = ValidationRules.EMAIL_HAS_SPACE)
        @Pattern(regexp = ValidationRules.EMAIL_ONE_AT_PATTERN, message = ValidationRules.EMAIL_ONE_AT)
        @Pattern(regexp = ValidationRules.EMAIL_DOMAIN_PATTERN, message = ValidationRules.EMAIL_NO_DOMAIN)
        @Pattern(regexp = ValidationRules.EMAIL_PATTERN, message = ValidationRules.EMAIL_INVALID)
        String email,

        @NotBlank(message = "Initial password is required")
        @StrongPassword
        String initialPassword,

        // optional for an admin: empty becomes null and passes
        @Pattern(regexp = ValidationRules.MOBILE_DIGITS_PATTERN, message = ValidationRules.MOBILE_DIGITS_ONLY)
        @Pattern(regexp = ValidationRules.MOBILE_LENGTH_PATTERN, message = ValidationRules.MOBILE_WRONG_LENGTH)
        @Pattern(regexp = ValidationRules.MOBILE_START_PATTERN, message = ValidationRules.MOBILE_START)
        String mobileNumber) {

    // the password stays as typed
    public AddAdminRequest {
        username = ValidationRules.clean(username);
        email = ValidationRules.clean(email);
        mobileNumber = ValidationRules.clean(mobileNumber);
    }

    // no password in log lines
    @Override
    public String toString() {
        return "AddAdminRequest[email=" + email + ", username=" + username + "]";
    }
}
