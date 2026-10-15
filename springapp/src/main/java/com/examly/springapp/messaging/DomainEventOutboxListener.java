package com.examly.springapp.messaging;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.gridpulse.common.event.AdminInvited;
import com.gridpulse.common.event.BookingDecided;
import com.gridpulse.common.event.BookingPromoted;
import com.gridpulse.common.event.BookingRefundRequested;
import com.gridpulse.common.event.BookingWaitlisted;
import com.gridpulse.common.event.OtpRequested;
import com.gridpulse.common.event.Topics;
import com.gridpulse.common.outbox.EventPublisher;

import lombok.RequiredArgsConstructor;

// the services' events -> outbox rows, still inside their transaction (BEFORE_COMMIT): saved with the change or not at all
// the relay (common) sends them to kafka, notification-service emails them, payment-service refunds. the key keeps one account / booking in order
@Component
@RequiredArgsConstructor
public class DomainEventOutboxListener {

    private final EventPublisher eventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onOtpRequested(OtpRequested event) {
        eventPublisher.publish(Topics.ACCOUNT, event.email(), event);
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onAdminInvited(AdminInvited event) {
        eventPublisher.publish(Topics.ACCOUNT, event.email(), event);
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onBookingDecided(BookingDecided event) {
        eventPublisher.publish(Topics.BOOKING, String.valueOf(event.bookingId()), event);
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onBookingWaitlisted(BookingWaitlisted event) {
        eventPublisher.publish(Topics.BOOKING, String.valueOf(event.bookingId()), event);
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onBookingPromoted(BookingPromoted event) {
        eventPublisher.publish(Topics.BOOKING, String.valueOf(event.bookingId()), event);
    }

    // payment-service sends the money back
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onBookingRefundRequested(BookingRefundRequested event) {
        eventPublisher.publish(Topics.BOOKING, String.valueOf(event.bookingId()), event);
    }
}
