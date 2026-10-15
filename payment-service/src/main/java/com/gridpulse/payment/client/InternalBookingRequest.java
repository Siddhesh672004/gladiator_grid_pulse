package com.gridpulse.payment.client;

import com.gridpulse.payment.dto.request.RaceBookingRequest;

// POST /internal/bookings: the booking form + the paid order (the core saves one booking per order)
public record InternalBookingRequest(RaceBookingRequest booking, String razorpayOrderId, String razorpayPaymentId,
        Double amount) {
}
