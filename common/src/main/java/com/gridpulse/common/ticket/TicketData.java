package com.gridpulse.common.ticket;

import java.time.LocalDate;
import java.time.LocalTime;

// everything printed on the e-ticket
public record TicketData(long bookingId, String code, String driverName, String trackName, String trackLocation,
        LocalDate sessionDate, LocalTime startTime, LocalTime endTime, Integer carNumber, String carCategory) {
}
