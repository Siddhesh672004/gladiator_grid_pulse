package com.gridpulse.common.event;

// core: a paid booking was rejected / cancelled, payment-service sends the money of this order back
public record BookingRefundRequested(String orderId, Long bookingId, Double amount, String reason) {
}
