package com.gridpulse.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import com.gridpulse.common.event.BookingPromoted;
import com.gridpulse.common.event.BookingRefundRequested;
import com.gridpulse.common.event.BookingWaitlisted;
import com.gridpulse.core.account.data.User;
import com.gridpulse.core.booking.data.BookingStatus;
import com.gridpulse.core.booking.data.PaymentStatus;
import com.gridpulse.core.session.data.RaceSession;
import com.gridpulse.core.support.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;

// a full session keeps paid bookings on its waitlist: a seat that opens moves the oldest one up,
// the start of the session rejects and refunds whoever is still waiting
class WaitlistIntegrationTest extends IntegrationTest {

    @Test
    void fullWaitlistedPromotedThenTheRestRefunded() throws Exception {
        RaceSession session = data.practiceSession(1);
        String admin = data.token(data.admin());
        User first = data.driver();
        Long firstBooking = book(first, session, 1);
        assertThat(status(approve(admin, firstBooking))).isEqualTo(200);

        User second = data.driver();
        User third = data.driver();
        JsonNode secondBooking = json(createPaid(second, session, 2, newOrderId()));
        JsonNode thirdBooking = json(createPaid(third, session, 3, newOrderId()));
        assertThat(secondBooking.get("status").asText()).isEqualTo(BookingStatus.WAITLISTED);
        assertThat(secondBooking.get("waitlistPosition").asInt()).isEqualTo(1);
        assertThat(thirdBooking.get("waitlistPosition").asInt()).isEqualTo(2);
        Long secondId = secondBooking.get("raceBookingId").asLong();
        Long thirdId = thirdBooking.get("raceBookingId").asLong();
        assertThat(outboxEvents(BookingWaitlisted.class)).filteredOn(event -> event.bookingId().equals(thirdId))
                .singleElement()
                .satisfies(event -> assertThat(event.note()).contains("number 2"));
        // the quote tells payment-service before the driver pays
        Map<String, Object> fourth = Map.of("sessionId", session.getSessionId(), "carNumber", 4, "carCategory", "GT3",
                "termsAccepted", true);
        assertThat(json(postInternal("/internal/bookings/check", data.token(data.driver()), fourth))
                .get("waitlisted").asBoolean()).isTrue();
        // nobody jumps the queue
        assertThat(status(approve(admin, secondId))).isEqualTo(409);

        // the approved driver cancels (no refund for an approved booking): the first in the queue moves up
        assertThat(status(delete("/api/race-booking/" + firstBooking, data.token(first)))).isEqualTo(204);
        assertThat(data.booking(secondId).getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(outboxEvents(BookingPromoted.class)).extracting(BookingPromoted::bookingId).contains(secondId);
        ResponseEntity<String> stillWaiting = get("/api/race-booking/" + thirdId, data.token(third));
        assertThat(json(stillWaiting).get("waitlistPosition").asInt()).isEqualTo(1);

        // approved, on track: the session starts and the waitlist is sent away with its money
        assertThat(status(approve(admin, secondId))).isEqualTo(200);
        assertThat(status(post("/api/race-control/" + secondId + "/start", admin, null))).isEqualTo(200);
        assertThat(data.booking(thirdId).getStatus()).isEqualTo(BookingStatus.REJECTED);
        assertThat(data.booking(thirdId).getPaymentStatus()).isEqualTo(PaymentStatus.REFUND_PENDING);
        assertThat(outboxEvents(BookingRefundRequested.class)).extracting(BookingRefundRequested::bookingId)
                .contains(thirdId)
                .doesNotContain(firstBooking);
    }

    @Test
    void approvingTheLastSeatWaitlistsTheOthersWithoutRefund() throws Exception {
        RaceSession session = data.practiceSession(1);
        String admin = data.token(data.admin());
        Long chosen = book(data.driver(), session, 11);
        Long waiting = book(data.driver(), session, 12);

        assertThat(status(approve(admin, chosen))).isEqualTo(200);

        assertThat(data.booking(waiting).getStatus()).isEqualTo(BookingStatus.WAITLISTED);
        assertThat(data.booking(waiting).getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(outboxEvents(BookingWaitlisted.class)).extracting(BookingWaitlisted::bookingId).contains(waiting);
        // an admin may still reject a waitlisted booking, the driver gets the money back
        assertThat(status(put("/api/race-booking/" + waiting, admin,
                Map.of("status", "Rejected", "adminNote", "Not this time")))).isEqualTo(200);
        assertThat(data.booking(waiting).getPaymentStatus()).isEqualTo(PaymentStatus.REFUND_PENDING);
    }
}
