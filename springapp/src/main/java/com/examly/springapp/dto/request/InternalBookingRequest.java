package com.examly.springapp.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// payment-service, after the money came in: make the booking for this paid order
public record InternalBookingRequest(

        @NotNull
        @Valid
        RaceBookingRequest booking,

        @NotBlank
        String razorpayOrderId,

        @NotBlank
        String razorpayPaymentId,

        @NotNull
        Double amount) {
}
