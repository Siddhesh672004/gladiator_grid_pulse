package com.gridpulse.core.booking.dto;

// created = a new booking (201), false = this order already had its booking (200, a retry)
public record InternalBookingResult(RaceBookingResponse booking, boolean created) {
}
