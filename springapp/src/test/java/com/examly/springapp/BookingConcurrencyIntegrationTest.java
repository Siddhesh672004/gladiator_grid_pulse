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

// the session row lock: two requests at the same moment, exactly one wins, the money of the other goes back
class BookingConcurrencyIntegrationTest extends IntegrationTest {

    @Test
    void sameCarNumberTwoDriversConcurrentConfirm() throws Exception {
        RaceSession session = data.practiceSession(10);
        User first = data.driver();
        User second = data.driver();
        String firstOrder = createOrder(first, session, 7);
        String secondOrder = createOrder(second, session, 7);

        List<ResponseEntity<String>> answers = atTheSameTime(
                () -> confirm(first, session, 7, firstOrder),
                () -> confirm(second, session, 7, secondOrder));

        assertThat(answers).extracting(IntegrationTest::status).containsExactlyInAnyOrder(201, 409);
        assertThat(data.bookingsOf(session)).hasSize(1);
        boolean firstWon = status(answers.get(0)) == 201;
        String winningOrder = firstWon ? firstOrder : secondOrder;
        String losingOrder = firstWon ? secondOrder : firstOrder;
        assertThat(data.payment(winningOrder).getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(data.payment(losingOrder).getStatus()).isEqualTo(PaymentStatus.REFUND_PENDING);
    }

    @Test
    void sameOrderConfirmedTwice() throws Exception {
        RaceSession session = data.practiceSession(10);
        User driver = data.driver();
        String order = createOrder(driver, session, 11);

        ResponseEntity<String> first = confirm(driver, session, 11, order);
        ResponseEntity<String> second = confirm(driver, session, 11, order);

        assertThat(status(first)).isEqualTo(201);
        assertThat(second.getStatusCode().is4xxClientError()).isTrue();
        assertThat(data.bookingsOf(session)).hasSize(1);
        assertThat(data.payment(order).getStatus()).isEqualTo(PaymentStatus.PAID);
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
        // the approval that filled the seat sent the waiting booking away with a refund
        RaceBooking rejected = data.booking(loser);
        assertThat(rejected.getStatus()).isEqualTo(BookingStatus.REJECTED);
        assertThat(rejected.getPayment().getStatus()).isEqualTo(PaymentStatus.REFUND_PENDING);
    }
}
