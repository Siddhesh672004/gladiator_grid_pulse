package com.gridpulse.common.event;

import java.time.LocalDate;

// payment-service: a payment changed its status (bookingId null = no booking yet). the core copies the status onto
// the booking, notification-service emails "refund done" on REFUNDED
public record PaymentStatusChanged(String orderId, Long bookingId, String status, Double amount, String driverEmail,
        String driverName, String trackName, LocalDate sessionDate, String note) {
}
