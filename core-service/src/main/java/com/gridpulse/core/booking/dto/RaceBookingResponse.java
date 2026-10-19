package com.gridpulse.core.booking.dto;

import java.time.Instant;

import com.gridpulse.core.account.dto.UserResponse;
import com.gridpulse.core.booking.data.PaymentStatus;
import com.gridpulse.core.session.dto.SessionResponse;

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
