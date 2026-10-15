package com.gridpulse.payment.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// sent right after the driver paid in the razorpay window, the signature proves the payment is real
public record PaymentConfirmRequest(

        @NotNull(message = "Booking details are required")
        @Valid
        RaceBookingRequest booking,

        @NotBlank(message = "Razorpay order id is required")
        String razorpayOrderId,

        @NotBlank(message = "Razorpay payment id is required")
        String razorpayPaymentId,

        @NotBlank(message = "Razorpay signature is required")
        String razorpaySignature) {
}
