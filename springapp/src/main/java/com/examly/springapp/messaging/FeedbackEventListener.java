package com.examly.springapp.messaging;

import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.examly.springapp.service.FeedbackService;
import com.gridpulse.common.event.EventEnvelope;
import com.gridpulse.common.event.EventReader;
import com.gridpulse.common.event.FeedbackLabelled;
import com.gridpulse.common.event.Topics;
import com.gridpulse.common.inbox.Inbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// ai-service's label of a new feedback (FeedbackLabelled) -> the feedback row; urgent -> UrgentFeedbackReported
// one group for every core instance, inbox + row change (+ the urgent event) in one transaction
@Slf4j
@Component
@RequiredArgsConstructor
public class FeedbackEventListener {

    static final String CONSUMER = "core-service";

    private final EventReader eventReader;
    private final Inbox inbox;
    private final FeedbackService feedbackService;
    private final TransactionTemplate transactionTemplate;

    @RetryableTopic(attempts = "4", backoff = @Backoff(delay = 2000, multiplier = 3), numPartitions = "3",
            retryTopicSuffix = "-core-retry", dltTopicSuffix = "-core-dlt")
    @KafkaListener(topics = Topics.FEEDBACK, groupId = CONSUMER, properties = "auto.offset.reset=earliest")
    public void onFeedbackEvent(String message) {
        EventEnvelope event = eventReader.read(message);
        if (!"FeedbackLabelled".equals(event.type())) {
            log.debug("Event type {} is not for the core, skipped", event.type());
            return;
        }
        FeedbackLabelled label = eventReader.payload(event, FeedbackLabelled.class);
        transactionTemplate.executeWithoutResult(status -> {
            if (!inbox.isNew(event.eventId(), CONSUMER)) {
                log.debug("Event {} was already handled", event.eventId());
                return;
            }
            feedbackService.applyLabel(label.feedbackId(), label.sentiment(), label.urgent());
            inbox.markProcessed(event.eventId(), CONSUMER);
        });
    }

    // every try failed: only the event id and type go to the log, never the payload
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
