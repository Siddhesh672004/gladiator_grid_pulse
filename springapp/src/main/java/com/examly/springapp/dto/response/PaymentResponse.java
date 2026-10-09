package com.examly.springapp.dto.response;

import java.time.Instant;

import com.examly.springapp.model.PaymentStatus;

public record PaymentResponse(
        Long paymentId,
        UserResponse user,
        SessionResponse session,
        Double amount,                 // rupees
        PaymentStatus status,
        String razorpayOrderId,
        String razorpayPaymentId,
        String razorpayRefundId,
        String note,                   // why the money goes back / why a refund failed
        Instant createdAt,
        Instant paidAt,
        Instant refundedAt) {
}
