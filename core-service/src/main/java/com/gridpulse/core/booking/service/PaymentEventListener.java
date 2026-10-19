package com.gridpulse.core.booking.service;

import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.gridpulse.common.event.EventEnvelope;
import com.gridpulse.common.event.EventReader;
import com.gridpulse.common.event.PaymentStatusChanged;
import com.gridpulse.common.event.Topics;
import com.gridpulse.common.inbox.Inbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// payment-service's events: the booking of that order copies the payment status (the badge on the booking pages)
// one group for every core instance (each event is handled once), inbox + booking change in one transaction
// failed -> tried again after 2 s, 6 s and 18 s on the core's own retry topics, then the dead letter topic
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    static final String CONSUMER = "core-service";

    private final EventReader eventReader;
    private final Inbox inbox;
    private final RaceBookingService raceBookingService;
    private final TransactionTemplate transactionTemplate;

    @RetryableTopic(attempts = "4", backoff = @Backoff(delay = 2000, multiplier = 3), numPartitions = "3",
            retryTopicSuffix = "-core-retry", dltTopicSuffix = "-core-dlt")
    @KafkaListener(topics = Topics.PAYMENT, groupId = CONSUMER, properties = "auto.offset.reset=earliest")
    public void onPaymentEvent(String message) {
        EventEnvelope event = eventReader.read(message);
        if ("PaymentStatusChanged".equals(event.type())) {
            PaymentStatusChanged change = eventReader.payload(event, PaymentStatusChanged.class);
            transactionTemplate.executeWithoutResult(status -> once(event,
                    () -> raceBookingService.applyPaymentStatus(change.orderId(), change.status())));
        } else {
            log.debug("Event type {} is not for the core, skipped", event.type());
        }
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

    private void once(EventEnvelope event, Runnable work) {
        if (!inbox.isNew(event.eventId(), CONSUMER)) {
            log.debug("Event {} was already handled", event.eventId());
            return;
        }
        work.run();
        inbox.markProcessed(event.eventId(), CONSUMER);
    }
}
