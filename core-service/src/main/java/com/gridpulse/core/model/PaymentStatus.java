package com.gridpulse.core.model;

// CREATED -> CAPTURED (paid, booking not saved yet) -> PAID -> REFUND_PENDING -> REFUNDED (or REFUND_FAILED, retry)
// paid but the booking was refused: CREATED -> REFUND_PENDING. never paid for 15 minutes: CREATED -> FAILED
public enum PaymentStatus {
    CREATED,
    CAPTURED,
    PAID,
    FAILED,
    REFUND_PENDING,
    REFUNDED,
    REFUND_FAILED
}
