package com.gridpulse.payment.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// the core's booking form, same rules and messages: a bad form gets the same 400 before razorpay is asked
// passed on to the core unchanged, which checks it again together with the database rules
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
}
