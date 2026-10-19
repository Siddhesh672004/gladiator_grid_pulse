package com.gridpulse.ai.feedback.service;

import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.gridpulse.ai.feedback.dto.FeedbackLabel;
import com.gridpulse.common.event.EventEnvelope;
import com.gridpulse.common.event.EventReader;
import com.gridpulse.common.event.FeedbackLabelled;
import com.gridpulse.common.event.FeedbackSubmitted;
import com.gridpulse.common.event.Topics;
import com.gridpulse.common.inbox.Inbox;
import com.gridpulse.common.outbox.EventPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// a saved feedback (FeedbackSubmitted) -> groq labels it -> FeedbackLabelled back to the core (outbox)
// the label call runs outside the transaction; inbox row + outbox row are saved together, so a second delivery
// publishes nothing. failed -> 2 s, 6 s, 18 s on this service's own retry topics, then the dead letter topic
@Slf4j
@Component
@RequiredArgsConstructor
public class FeedbackEventListener {

    static final String CONSUMER = "ai-service";

    private final EventReader eventReader;
    private final Inbox inbox;
    private final FeedbackAiService feedbackAiService;
    private final EventPublisher eventPublisher;
    private final TransactionTemplate transactionTemplate;

    @RetryableTopic(attempts = "4", backoff = @Backoff(delay = 2000, multiplier = 3), numPartitions = "3",
            retryTopicSuffix = "-ai-retry", dltTopicSuffix = "-ai-dlt")
    @KafkaListener(topics = Topics.FEEDBACK, groupId = CONSUMER)
    public void onFeedbackEvent(String message) {
        EventEnvelope event = eventReader.read(message);
        if (!"FeedbackSubmitted".equals(event.type())) {
            log.debug("Event type {} is not for ai-service, skipped", event.type());
            return;
        }
        if (!inbox.isNew(event.eventId(), CONSUMER)) {
            log.debug("Event {} was already handled", event.eventId());
            return;
        }
        FeedbackSubmitted feedback = eventReader.payload(event, FeedbackSubmitted.class);
        FeedbackLabel label = feedbackAiService.label(feedback.text());
        transactionTemplate.executeWithoutResult(status -> {
            // a delivery that raced this one may have finished first
            if (inbox.isNew(event.eventId(), CONSUMER)) {
                eventPublisher.publish(Topics.FEEDBACK, String.valueOf(feedback.feedbackId()),
                        new FeedbackLabelled(feedback.feedbackId(), label.sentiment().name(), label.urgent()));
                inbox.markProcessed(event.eventId(), CONSUMER);
            }
        });
    }

    // every try failed: only the event id and type go to the log, never the payload (the feedback text)
    @DltHandler
    public void onDeadLetter(String message, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        try {
            EventEnvelope event = eventReader.read(message);
            log.error("Event {} ({}) gave up after 4 tries, it is on {}", event.eventId(), event.type(), topic);
        } catch (IllegalArgumentException e) {
            log.error("A message that is not an event landed on {}", topic);
        }
    }
}
