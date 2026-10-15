package com.examly.springapp.event;

import java.time.LocalDate;
import java.time.LocalTime;

// a booking became Approved or Rejected. refundAmount null = no money goes back
public record BookingDecided(Long bookingId, String driverEmail, String driverName, String trackName,
        String trackLocation, LocalDate sessionDate, LocalTime startTime, LocalTime endTime, Integer carNumber,
        String carCategory, String status, String note, Double refundAmount) {
}
