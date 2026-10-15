package com.examly.springapp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import com.examly.springapp.model.BookingStatus;
import com.examly.springapp.model.PaymentStatus;
import com.examly.springapp.model.RaceBooking;
import com.examly.springapp.model.RaceSession;
import com.examly.springapp.model.User;
import com.examly.springapp.support.IntegrationTest;
import com.gridpulse.common.event.BookingRefundRequested;

// the session row lock: two paid bookings at the same moment, exactly one wins (payment-service refunds the other)
class BookingConcurrencyIntegrationTest extends IntegrationTest {

    @Test
    void sameCarNumberTwoDriversAtOnce() throws Exception {
        RaceSession session = data.practiceSession(10);
        User first = data.driver();
        User second = data.driver();
        String firstOrder = newOrderId();
        String secondOrder = newOrderId();

        List<ResponseEntity<String>> answers = atTheSameTime(
                () -> createPaid(first, session, 7, firstOrder),
                () -> createPaid(second, session, 7, secondOrder));

        assertThat(answers).extracting(IntegrationTest::status).containsExactlyInAnyOrder(201, 409);
        assertThat(data.bookingsOf(session)).hasSize(1);
        boolean firstWon = status(answers.get(0)) == 201;
        String winningOrder = firstWon ? firstOrder : secondOrder;
        // the loser's order made no booking: payment-service refunds it
        assertThat(data.bookingsOf(session)).extracting(RaceBooking::getPaymentOrderId).containsExactly(winningOrder);
        assertThat(data.bookingOfOrder(winningOrder).getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(json(firstWon ? answers.get(1) : answers.get(0)).get("message").asText())
                .contains("Car number 7");
    }

    @Test
    void sameOrderTwiceGivesTheSameBooking() throws Exception {
        RaceSession session = data.practiceSession(10);
        User driver = data.driver();
        String order = newOrderId();

        ResponseEntity<String> first = createPaid(driver, session, 11, order);
        ResponseEntity<String> second = createPaid(driver, session, 11, order);

        assertThat(status(first)).isEqualTo(201);
        assertThat(status(second)).isEqualTo(200);
        assertThat(json(second).get("raceBookingId").asLong()).isEqualTo(json(first).get("raceBookingId").asLong());
        assertThat(data.bookingsOf(session)).hasSize(1);
        assertThat(data.bookingOfOrder(order).getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    void twoAdminsApproveLastSeatConcurrently() throws Exception {
        RaceSession session = data.practiceSession(1);
        Long firstBooking = book(data.driver(), session, 21);
        Long secondBooking = book(data.driver(), session, 22);
        String firstAdmin = data.token(data.admin());
        String secondAdmin = data.token(data.admin());

        List<ResponseEntity<String>> answers = atTheSameTime(
                () -> approve(firstAdmin, firstBooking),
                () -> approve(secondAdmin, secondBooking));

        assertThat(answers).extracting(IntegrationTest::status).containsExactlyInAnyOrder(200, 409);
        boolean firstWon = status(answers.get(0)) == 200;
        Long winner = firstWon ? firstBooking : secondBooking;
        Long loser = firstWon ? secondBooking : firstBooking;
        assertThat(data.booking(winner).getStatus()).isEqualTo(BookingStatus.APPROVED);
        // the approval that filled the seat sent the waiting booking away, payment-service is asked for the refund
        RaceBooking rejected = data.booking(loser);
        assertThat(rejected.getStatus()).isEqualTo(BookingStatus.REJECTED);
        assertThat(rejected.getPaymentStatus()).isEqualTo(PaymentStatus.REFUND_PENDING);
        assertThat(outboxEvents(BookingRefundRequested.class))
                .anySatisfy(refund -> {
                    assertThat(refund.bookingId()).isEqualTo(loser);
                    assertThat(refund.orderId()).isEqualTo(rejected.getPaymentOrderId());
                    assertThat(refund.amount()).isEqualTo(500.0);
                });
    }
}
