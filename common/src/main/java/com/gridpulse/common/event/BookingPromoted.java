package com.gridpulse.common.event;

import java.time.LocalDate;
import java.time.LocalTime;

// a seat opened up: a waitlisted booking is Pending again and waits for the admin's approval
public record BookingPromoted(Long bookingId, String driverEmail, String driverName, String trackName,
        String trackLocation, LocalDate sessionDate, LocalTime startTime, LocalTime endTime, Integer carNumber,
        String carCategory, String status, String note, Double refundAmount) {
}
