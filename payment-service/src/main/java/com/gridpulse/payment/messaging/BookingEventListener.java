package com.gridpulse.payment.messaging;

import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.gridpulse.common.event.BookingRefundRequested;
import com.gridpulse.common.event.EventEnvelope;
import com.gridpulse.common.event.EventReader;
import com.gridpulse.common.event.Topics;
import com.gridpulse.common.inbox.Inbox;
import com.gridpulse.payment.service.PaymentRecordService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// the core's booking events: a rejected / cancelled paid booking wants its money back (BookingRefundRequested)
// inbox check, payment change and its PaymentStatusChanged event share one transaction: a second delivery does nothing
// failed -> tried again after 2 s, 6 s and 18 s on this service's own retry topics, then the dead letter topic
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingEventListener {

    static final String CONSUMER = "payment-service";

    private final EventReader eventReader;
    private final Inbox inbox;
    private final PaymentRecordService paymentRecordService;
    private final TransactionTemplate transactionTemplate;

    @RetryableTopic(attempts = "4", backoff = @Backoff(delay = 2000, multiplier = 3), numPartitions = "3",
            retryTopicSuffix = "-payment-retry", dltTopicSuffix = "-payment-dlt")
    @KafkaListener(topics = Topics.BOOKING, groupId = CONSUMER)
    public void onBookingEvent(String message) {
        EventEnvelope event = eventReader.read(message);
        if ("BookingRefundRequested".equals(event.type())) {
            BookingRefundRequested request = eventReader.payload(event, BookingRefundRequested.class);
            transactionTemplate.executeWithoutResult(status -> once(event, () -> paymentRecordService.refundForBooking(request)));
        } else {
            log.debug("Event type {} is not for payment-service, skipped", event.type());
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
