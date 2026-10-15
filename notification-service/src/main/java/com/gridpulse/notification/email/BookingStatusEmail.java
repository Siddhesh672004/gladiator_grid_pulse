package com.gridpulse.notification.email;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

// sent when a booking becomes Approved or Rejected
// note = the admin's reason or "Session is full"; refundAmount null = no refund line
public record BookingStatusEmail(String username, String trackName, LocalDate sessionDate, LocalTime startTime,
        LocalTime endTime, Integer carNumber, String status, String note, Double refundAmount)
        implements EmailMessage {

    @Override
    public String subject() {
        return "Your GridPulse booking was " + status.toLowerCase();
    }

    @Override
    public String body() {
        // an empty note leaves its line out
        String noteLine = Optional.ofNullable(note)
                .filter(text -> !text.isBlank())
                .map(text -> "Note from GridPulse: " + text + "\n")
                .orElse("");
        // no refund = no refund line
        String refundLine = Optional.ofNullable(refundAmount)
                .map(amount -> "Your payment of Rs." + new DecimalFormat("0.##").format(amount)
                        + " will be refunded to the way you paid.\n")
                .orElse("");
        // an approval comes with the e-ticket and the calendar file (NotificationListener attaches them)
        String ticketLine = "approved".equalsIgnoreCase(status)
                ? "Your e-ticket is attached: show its QR code at the gate. The .ics file puts the session in your calendar.\n"
                : "";
        return """
                Hi %s,

                Your booking for car #%d on %s, %s %s - %s is now: %s
                %s%s%s
                You can see it under My Registrations.

                - GridPulse
                """.formatted(username, carNumber, trackName, sessionDate, startTime, endTime, status.toUpperCase(),
                noteLine, refundLine, ticketLine);
    }
}
