package com.gridpulse.core.account.dto;

import com.gridpulse.core.platform.validation.ValidationRules;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EmailRequest(

        @NotBlank(message = ValidationRules.EMAIL_REQUIRED)
        @Size(max = ValidationRules.EMAIL_MAX, message = ValidationRules.EMAIL_TOO_LONG)
        @Pattern(regexp = ValidationRules.EMAIL_NO_SPACE_PATTERN, message = ValidationRules.EMAIL_HAS_SPACE)
        @Pattern(regexp = ValidationRules.EMAIL_ONE_AT_PATTERN, message = ValidationRules.EMAIL_ONE_AT)
        @Pattern(regexp = ValidationRules.EMAIL_DOMAIN_PATTERN, message = ValidationRules.EMAIL_NO_DOMAIN)
        @Pattern(regexp = ValidationRules.EMAIL_PATTERN, message = ValidationRules.EMAIL_INVALID)
        String email) {

    public EmailRequest {
        email = ValidationRules.clean(email);
    }
}
