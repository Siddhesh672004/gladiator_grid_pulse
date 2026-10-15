package com.gridpulse.common.event;

import java.time.LocalDate;
import java.time.LocalTime;

// a paid booking waits for a seat (the session is full); note = its place in the queue. no refund
public record BookingWaitlisted(Long bookingId, String driverEmail, String driverName, String trackName,
        String trackLocation, LocalDate sessionDate, LocalTime startTime, LocalTime endTime, Integer carNumber,
        String carCategory, String status, String note, Double refundAmount) {
}
