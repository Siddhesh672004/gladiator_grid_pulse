package com.examly.springapp.model;

// CREATED -> PAID -> REFUND_PENDING -> REFUNDED (or REFUND_FAILED, the admin can retry)
// paid but the booking was refused: CREATED -> REFUND_PENDING. never paid for 15 minutes: CREATED -> FAILED
public enum PaymentStatus {
    CREATED,
    PAID,
    FAILED,
    REFUND_PENDING,
    REFUNDED,
    REFUND_FAILED
}
