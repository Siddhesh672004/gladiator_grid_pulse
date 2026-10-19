package com.gridpulse.core.dto.request;

import java.time.LocalDate;

import com.gridpulse.core.validation.Adult;
import com.gridpulse.core.validation.StrongPassword;
import com.gridpulse.core.validation.ValidationRules;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// sent twice: /register/send-code (a code goes to the email), then /register with the same body + otp (account is made)
// no role: signing up always makes a driver, admins are added by the super admin
public record SignupRequest(

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

        @NotBlank(message = ValidationRules.MOBILE_REQUIRED)
        @Pattern(regexp = ValidationRules.MOBILE_DIGITS_PATTERN, message = ValidationRules.MOBILE_DIGITS_ONLY)
        @Pattern(regexp = ValidationRules.MOBILE_LENGTH_PATTERN, message = ValidationRules.MOBILE_WRONG_LENGTH)
        @Pattern(regexp = ValidationRules.MOBILE_START_PATTERN, message = ValidationRules.MOBILE_START)
        String mobileNumber,

        @NotNull(message = ValidationRules.DOB_REQUIRED)
        @Past(message = ValidationRules.DOB_IN_FUTURE)
        @Adult
        LocalDate dateOfBirth,

        @NotBlank(message = ValidationRules.LICENSE_REQUIRED)
        @Pattern(regexp = ValidationRules.LICENSE_START_PATTERN, message = ValidationRules.LICENSE_NO_DL)
        @Pattern(regexp = ValidationRules.LICENSE_DIGITS_PATTERN, message = ValidationRules.LICENSE_DIGITS)
        String driverLicenseNumber,

        @NotBlank(message = "Password is required")
        @StrongPassword
        String password,

        // empty in step 1, UserServiceImpl checks it in step 2
        String otp) {

    // texts lose the spaces around them before the checks, the password stays as typed
    public SignupRequest {
        username = ValidationRules.clean(username);
        email = ValidationRules.clean(email);
        mobileNumber = ValidationRules.clean(mobileNumber);
        driverLicenseNumber = ValidationRules.cleanUpper(driverLicenseNumber);
        otp = ValidationRules.clean(otp);
    }

    // no password / code in log lines
    @Override
    public String toString() {
        return "SignupRequest[email=" + email + ", username=" + username + "]";
    }
}
