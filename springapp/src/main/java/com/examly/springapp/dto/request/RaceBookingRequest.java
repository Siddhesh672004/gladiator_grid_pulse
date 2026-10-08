package com.examly.springapp.dto.request;

import com.examly.springapp.validation.ValidationRules;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// no userId and no license: the driver comes from the token, the license from the profile
// db rules (seat left, free car number ...) are checked in RaceBookingServiceImpl
public record RaceBookingRequest(

        @NotNull(message = "Please pick a session")
        Long sessionId,

        @NotNull(message = "Car number is required")
        @Min(value = 1, message = "Car number must be at least 1")
        @Max(value = 999, message = "Car number must be at most 999")
        Integer carNumber,

        @NotBlank(message = "Car category is required")
        String carCategory,

        @Size(max = 500, message = "Comments must be at most 500 characters")
        String comments,

        @NotNull(message = "Please accept the terms and conditions")
        @AssertTrue(message = "Please accept the terms and conditions")
        Boolean termsAccepted) {

    public RaceBookingRequest {
        comments = ValidationRules.clean(comments);
    }
}
