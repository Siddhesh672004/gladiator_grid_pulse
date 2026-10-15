package com.gridpulse.notification.email;

import java.time.LocalDate;
import java.time.LocalTime;

// sent when a seat opened up and a waitlisted booking is Pending again
public record PromotedEmail(String username, String trackName, LocalDate sessionDate, LocalTime startTime,
        LocalTime endTime, Integer carNumber) implements EmailMessage {

    @Override
    public String subject() {
        return "A seat opened up for your GridPulse booking";
    }

    @Override
    public String body() {
        return """
                Hi %s,

                A seat opened up: your booking for car #%d on %s, %s %s - %s has left the waitlist
                and now waits for the admin's approval.

                You can see it under My Registrations.

                - GridPulse
                """.formatted(username, carNumber, trackName, sessionDate, startTime, endTime);
    }
}
