package com.gridpulse.common.event;

import java.time.LocalDate;
import java.util.List;

// core: a feedback reports an injury, an accident or a safety problem; notification-service emails every admin
// excerpt = the start of the text (300 characters at most)
public record UrgentFeedbackReported(Long feedbackId, String driverName, String trackName, LocalDate sessionDate,
        Integer rating, String excerpt, List<String> adminEmails) {
}
