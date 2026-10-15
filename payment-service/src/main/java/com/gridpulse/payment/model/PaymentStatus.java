package com.gridpulse.payment.model;

// CREATED -> CAPTURED (paid, the core has not saved the booking yet) -> PAID -> REFUND_PENDING -> REFUNDED
// (or REFUND_FAILED, the admin can retry). booking refused after paying: CAPTURED -> REFUND_PENDING
// never paid for 15 minutes: CREATED -> FAILED
public enum PaymentStatus {
    CREATED,
    CAPTURED,
    PAID,
    FAILED,
    REFUND_PENDING,
    REFUNDED,
    REFUND_FAILED
}
