package com.gridpulse.core.dto.response;

// created = a new booking (201), false = this order already had its booking (200, a retry)
public record InternalBookingResult(RaceBookingResponse booking, boolean created) {
}
