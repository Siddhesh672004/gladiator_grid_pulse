package com.gridpulse.notification.email;

import java.time.LocalDate;
import java.time.LocalTime;

// sent when a paid booking goes on the waitlist (the session is full); note = its place in the queue
public record WaitlistEmail(String username, String trackName, LocalDate sessionDate, LocalTime startTime,
        LocalTime endTime, Integer carNumber, String note) implements EmailMessage {

    @Override
    public String subject() {
        return "Your GridPulse booking is on the waitlist";
    }

    @Override
    public String body() {
        return """
                Hi %s,

                Your booking for car #%d on %s, %s %s - %s is on the waitlist: %s.
                It moves up by itself as soon as a seat opens. If no seat opens before the session starts,
                your payment is refunded in full.

                You can see it under My Registrations.

                - GridPulse
                """.formatted(username, carNumber, trackName, sessionDate, startTime, endTime, note);
    }
}
