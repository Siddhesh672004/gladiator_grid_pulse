package com.examly.springapp.dto.response;

import java.time.Instant;

import com.examly.springapp.model.PaymentStatus;

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
        PaymentStatus paymentStatus) { // empty = no payment (old booking / race place)
}
