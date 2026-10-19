package com.gridpulse.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gridpulse.core.messaging.PaymentEventListener;
import com.gridpulse.core.model.PaymentStatus;
import com.gridpulse.core.model.RaceBooking;
import com.gridpulse.core.model.RaceSession;
import com.gridpulse.core.support.IntegrationTest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.gridpulse.common.event.EventEnvelope;
import com.gridpulse.common.event.EventJson;
import com.gridpulse.common.event.PaymentStatusChanged;

// payment-service's PaymentStatusChanged on the booking: kafka may deliver an event twice, it counts once
// (the kafka listener is off in the tests, the events are handed to it directly)
class PaymentEventIntegrationTest extends IntegrationTest {

    @Autowired
    private PaymentEventListener paymentEventListener;

    @Test
    void theBadgeFollowsThePaymentOnce() throws Exception {
        RaceSession session = data.practiceSession(5);
        Long bookingId = book(data.driver(), session, 41);
        String order = data.booking(bookingId).getPaymentOrderId();

        String refundPending = event(order, bookingId, "REFUND_PENDING");
        paymentEventListener.onPaymentEvent(refundPending);
        assertThat(paymentStatusOf(bookingId)).isEqualTo(PaymentStatus.REFUND_PENDING);

        paymentEventListener.onPaymentEvent(event(order, bookingId, "REFUNDED"));
        // the first event again: already handled, the booking stays REFUNDED
        paymentEventListener.onPaymentEvent(refundPending);
        assertThat(paymentStatusOf(bookingId)).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    void aLateRefundPendingDoesNotUndoRefunded() throws Exception {
        RaceSession session = data.practiceSession(5);
        Long bookingId = book(data.driver(), session, 43);
        String order = data.booking(bookingId).getPaymentOrderId();

        paymentEventListener.onPaymentEvent(event(order, bookingId, "REFUNDED"));
        // a retry topic delivers the older REFUND_PENDING afterwards (its own event id, so not a duplicate)
        paymentEventListener.onPaymentEvent(event(order, bookingId, "REFUND_PENDING"));

        assertThat(paymentStatusOf(bookingId)).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    void otherStatusesAndOrdersChangeNothing() throws Exception {
        RaceSession session = data.practiceSession(5);
        Long bookingId = book(data.driver(), session, 42);
        String order = data.booking(bookingId).getPaymentOrderId();

        // CAPTURED / PAID are set by the booking itself, an unknown order or status is skipped
        paymentEventListener.onPaymentEvent(event(order, bookingId, "CAPTURED"));
        paymentEventListener.onPaymentEvent(event(order, bookingId, "NOT_A_STATUS"));
        paymentEventListener.onPaymentEvent(event("order_nobody_has", null, "REFUNDED"));

        assertThat(paymentStatusOf(bookingId)).isEqualTo(PaymentStatus.PAID);
    }

    private PaymentStatus paymentStatusOf(Long bookingId) {
        RaceBooking booking = data.booking(bookingId);
        return booking.getPaymentStatus();
    }

    private static String event(String order, Long bookingId, String status) throws JsonProcessingException {
        PaymentStatusChanged change = new PaymentStatusChanged(order, bookingId, status, 500.0, "driver@gridpulse.test",
                "Driver", "Test Circuit", null, null);
        return EventJson.MAPPER.writeValueAsString(new EventEnvelope(UUID.randomUUID().toString(),
                "PaymentStatusChanged", Instant.now(), "payment-service", EventJson.MAPPER.valueToTree(change)));
    }
}
