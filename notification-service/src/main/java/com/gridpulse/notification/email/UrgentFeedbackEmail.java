package com.gridpulse.notification.email;

import java.time.LocalDate;
import java.util.Optional;

// sent to every admin when the ai finds an injury, an accident or a safety problem in a feedback
public record UrgentFeedbackEmail(Long feedbackId, String driverName, String trackName, LocalDate sessionDate,
        Integer rating, String excerpt) implements EmailMessage {

    @Override
    public String subject() {
        return "Urgent GridPulse feedback #" + feedbackId + " (" + trackName + ")";
    }

    @Override
    public String body() {
        // a feedback without stars says so
        String stars = Optional.ofNullable(rating)
                .map(value -> value + " of 5 stars")
                .orElse("no rating");
        return """
                Hello,

                The AI marked feedback #%d as urgent: it may report an injury, an accident or a safety problem.

                Driver: %s
                Session: %s, %s
                Rating: %s

                "%s"

                Please read it under Feedback on the admin pages and follow it up.

                - GridPulse
                """.formatted(feedbackId, driverName, trackName, sessionDate, stars, excerpt);
    }
}
