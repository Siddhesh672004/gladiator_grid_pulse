package com.gridpulse.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import com.gridpulse.core.messaging.FeedbackEventListener;
import com.gridpulse.core.model.Feedback;
import com.gridpulse.core.model.RaceSession;
import com.gridpulse.core.model.Sentiment;
import com.gridpulse.core.model.User;
import com.gridpulse.core.repository.FeedbackRepo;
import com.gridpulse.core.support.IntegrationTest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.gridpulse.common.event.EventEnvelope;
import com.gridpulse.common.event.EventJson;
import com.gridpulse.common.event.FeedbackLabelled;
import com.gridpulse.common.event.FeedbackSubmitted;
import com.gridpulse.common.event.UrgentFeedbackReported;

// feedback is saved at once (NOT_ANALYSED + FeedbackSubmitted); ai-service's label comes back later: the row
// takes it, and an urgent one tells the admins once (the kafka listener is off in the tests, events go to it directly)
class FeedbackEventIntegrationTest extends IntegrationTest {

    @Autowired
    private FeedbackEventListener feedbackEventListener;

    @Autowired
    private FeedbackRepo feedbackRepo;

    @Test
    void savedAtOnceLabelledLaterUrgentOnce() throws Exception {
        RaceSession session = data.practiceSession(5);
        User driver = data.driver();
        User admin = data.admin();
        Long bookingId = book(driver, session, 8);
        approve(data.token(admin), bookingId);
        data.complete(session);

        ResponseEntity<String> created = post("/api/feedback", data.token(driver), Map.of("sessionId",
                session.getSessionId(), "rating", 1, "feedbackText", "The barrier at turn 3 is broken, a car hit it"));
        assertThat(status(created)).isEqualTo(201);
        Long feedbackId = json(created).get("feedbackId").asLong();
        assertThat(feedbackRepo.findById(feedbackId).orElseThrow().getSentiment()).isEqualTo(Sentiment.NOT_ANALYSED);
        assertThat(outboxEvents(FeedbackSubmitted.class)).extracting(FeedbackSubmitted::feedbackId).contains(feedbackId);

        String label = labelled(feedbackId, "NEGATIVE", true);
        feedbackEventListener.onFeedbackEvent(label);
        // the same event again: already handled
        feedbackEventListener.onFeedbackEvent(label);

        Feedback feedback = feedbackRepo.findById(feedbackId).orElseThrow();
        assertThat(feedback.getSentiment()).isEqualTo(Sentiment.NEGATIVE);
        assertThat(feedback.isUrgent()).isTrue();
        assertThat(outboxEvents(UrgentFeedbackReported.class))
                .filteredOn(urgent -> urgent.feedbackId().equals(feedbackId))
                .singleElement()
                .satisfies(urgent -> {
                    assertThat(urgent.adminEmails()).contains(admin.getEmail());
                    assertThat(urgent.excerpt()).startsWith("The barrier at turn 3");
                });
    }

    private static String labelled(Long feedbackId, String sentiment, boolean urgent) throws JsonProcessingException {
        return EventJson.MAPPER.writeValueAsString(new EventEnvelope(UUID.randomUUID().toString(), "FeedbackLabelled",
                Instant.now(), "ai-service",
                EventJson.MAPPER.valueToTree(new FeedbackLabelled(feedbackId, sentiment, urgent))));
    }
}
