package com.gridpulse.core.dto.response;

import java.time.Instant;

import com.gridpulse.core.model.PaymentStatus;

public record RaceBookingResponse(
        Long raceBookingId,
        UserResponse user,
        SessionResponse session,
        String status,
        Integer carNumber,
        String carCategory,
        String driverLicenseNumber,
        String comments,
        String adminNote,
        Double totalPrice,
        Instant termsAcceptedAt,
        Integer lapCount,
        Instant lastLapAt,
        String raceState,
        Integer finalPosition,
        Integer gridPosition,
        PaymentStatus paymentStatus,   // empty = no payment (old booking / race place)
        Integer waitlistPosition) {    // 1 = next to move up, empty when not Waitlisted
}
