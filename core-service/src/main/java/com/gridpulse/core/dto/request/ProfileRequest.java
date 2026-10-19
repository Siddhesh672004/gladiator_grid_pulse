package com.gridpulse.core.dto.request;

import java.time.LocalDate;

import com.gridpulse.core.validation.Adult;
import com.gridpulse.core.validation.ValidationRules;

import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// nothing is required: an admin has no date of birth / license, ProfileServiceImpl decides who must send them
public record ProfileRequest(

        @Past(message = ValidationRules.DOB_IN_FUTURE)
        @Adult
        LocalDate dateOfBirth,

        // the page leaves a saved (locked) license out, so an old one never meets the DL rule
        @Pattern(regexp = ValidationRules.LICENSE_START_PATTERN, message = ValidationRules.LICENSE_NO_DL)
        @Pattern(regexp = ValidationRules.LICENSE_DIGITS_PATTERN, message = ValidationRules.LICENSE_DIGITS)
        String driverLicenseNumber,

        @Size(max = 500, message = "Profile picture link must be at most 500 characters")
        String profilePicture) {

    public ProfileRequest {
        driverLicenseNumber = ValidationRules.cleanUpper(driverLicenseNumber);
        profilePicture = ValidationRules.clean(profilePicture);
    }
}
