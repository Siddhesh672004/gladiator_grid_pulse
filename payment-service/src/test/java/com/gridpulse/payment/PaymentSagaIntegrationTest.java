package com.gridpulse.payment;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import com.gridpulse.common.event.BookingRefundRequested;
import com.gridpulse.payment.client.CoreCalls;
import com.gridpulse.payment.messaging.BookingEventListener;
import com.gridpulse.payment.model.Payment;
import com.gridpulse.payment.model.PaymentStatus;
import com.gridpulse.payment.service.PaymentAuditService;
import com.gridpulse.payment.service.PaymentRecordService;
import com.gridpulse.payment.service.RazorpayService;
import com.gridpulse.payment.service.RefundJob;
import com.gridpulse.payment.support.FakeRazorpayService;
import com.gridpulse.payment.support.PaymentIntegrationTest;

// the confirm saga against a fake core: booked -> PAID, refused -> money back, no answer -> 202 and the job finishes it
class PaymentSagaIntegrationTest extends PaymentIntegrationTest {

    private static final String BOOKING = """
            {"raceBookingId": 41, "carNumber": 7, "status": "Pending", "paymentStatus": "PAID", "totalPrice": 650.0}""";

    @Autowired
    private PaymentRecordService paymentRecordService;

    @Autowired
    private PaymentAuditService paymentAuditService;

    @Autowired
    private RazorpayService razorpayService;

    @Autowired
    private CoreCalls coreCalls;

    @Autowired
    private BookingEventListener bookingEventListener;

    @Autowired
    private FakeRazorpayService fakeRazorpay;

    @Test
    void bookedByTheCoreMeansPaid() throws Exception {
        String driver = token(501, "User");
        String order = order(501, driver);
        CORE.stubFor(post(urlEqualTo("/internal/bookings")).willReturn(okJson(BOOKING).withStatus(201)));

        ResponseEntity<String> confirmed = confirm(driver, 7, order);

        assertThat(status(confirmed)).isEqualTo(201);
        assertThat(json(confirmed).get("raceBookingId").asLong()).isEqualTo(41);
        Payment payment = payment(order);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getBookingId()).isEqualTo(41);
        assertThat(announcedStatuses(order)).containsExactly("CAPTURED", "PAID");
        // the core got the internal key and the driver's own token
        CORE.verify(postRequestedFor(urlEqualTo("/internal/bookings"))
                .withHeader("X-Internal-Key", equalTo(INTERNAL_KEY))
                .withHeader("Authorization", matching("Bearer .+")));
        // a second click on the same payment books nothing
        assertThat(status(confirm(driver, 7, order))).isEqualTo(409);
        // somebody else's payment stays closed
        assertThat(status(confirm(token(502, "User"), 7, order(501, driver)))).isEqualTo(403);
    }

    @Test
    void refusedByTheCoreMeansRefund() throws Exception {
        String driver = token(511, "User");
        String order = order(511, driver);
        CORE.stubFor(post(urlEqualTo("/internal/bookings")).willReturn(aResponse().withStatus(409)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"status\":409,\"error\":\"Conflict\",\"message\":\"Car number 7 is already registered for this session\"}")));

        ResponseEntity<String> refused = confirm(driver, 7, order);

        assertThat(status(refused)).isEqualTo(409);
        assertThat(json(refused).get("message").asText()).isEqualTo("Car number 7 is already registered for this session");
        Payment payment = payment(order);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUND_PENDING);
        assertThat(payment.getNote()).startsWith("Paid, but the booking was refused");
        assertThat(announcedStatuses(order)).containsExactly("CAPTURED", "REFUND_PENDING");
    }

    @Test
    void coreTooSlowGives202AndTheJobFindsTheBooking() throws Exception {
        String driver = token(521, "User");
        String order = order(521, driver);
        // longer than the 5 s the core gets to answer (each of the 3 tries)
        CORE.stubFor(post(urlEqualTo("/internal/bookings")).willReturn(okJson(BOOKING).withStatus(201)
                .withFixedDelay(6000)));

        ResponseEntity<String> pending = confirm(driver, 7, order);

        assertThat(status(pending)).isEqualTo(202);
        assertThat(json(pending).get("message").asText()).contains("being confirmed");
        assertThat(payment(order).getStatus()).isEqualTo(PaymentStatus.CAPTURED);

        // a minute later the core answers again and has the booking
        capturedTwoMinutesAgo(order);
        CORE.stubFor(get(urlEqualTo("/internal/bookings/by-order/" + order)).willReturn(okJson(BOOKING)));
        refundJob().run();

        Payment payment = payment(order);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getBookingId()).isEqualTo(41);
    }

    @Test
    void coreDownAndNoBookingMeansRefund() throws Exception {
        String driver = token(531, "User");
        String order = order(531, driver);
        CORE.stubFor(post(urlEqualTo("/internal/bookings")).willReturn(aResponse().withStatus(503)));

        assertThat(status(confirm(driver, 7, order))).isEqualTo(202);

        capturedTwoMinutesAgo(order);
        CORE.stubFor(get(urlEqualTo("/internal/bookings/by-order/" + order)).willReturn(aResponse().withStatus(404)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"status\":404,\"message\":\"No booking for order " + order + "\"}")));
        refundJob().run();

        // found missing and refunded in the same run
        assertThat(payment(order).getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(announcedStatuses(order)).containsExactly("CAPTURED", "REFUND_PENDING", "REFUNDED");
    }

    @Test
    void refundAskedTwiceChangesOnce() throws Exception {
        String driver = token(541, "User");
        String order = order(541, driver);
        CORE.stubFor(post(urlEqualTo("/internal/bookings")).willReturn(okJson(BOOKING).withStatus(201)));
        assertThat(status(confirm(driver, 7, order))).isEqualTo(201);

        String rejected = envelope(new BookingRefundRequested(order, 41L, 650.0, "Booking rejected: car not ready"));
        bookingEventListener.onBookingEvent(rejected);
        bookingEventListener.onBookingEvent(rejected);
        // a second request with its own event id: the payment is no longer PAID, nothing happens
        bookingEventListener.onBookingEvent(envelope(new BookingRefundRequested(order, 41L, 650.0, "again")));

        Payment payment = payment(order);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUND_PENDING);
        assertThat(payment.getNote()).isEqualTo("Booking rejected: car not ready");
        assertThat(announcedStatuses(order)).containsExactly("CAPTURED", "PAID", "REFUND_PENDING");
    }

    @Test
    void oneBrokenRefundDoesNotStopTheOthers() throws Exception {
        String broken = refusedOrder(551);
        String fine = refusedOrder(552);
        fakeRazorpay.breakRefundOf("pay_" + broken);
        try {
            refundJob().run();
        } finally {
            fakeRazorpay.breakRefundOf(null);
        }

        assertThat(payment(broken).getStatus()).isEqualTo(PaymentStatus.REFUND_PENDING);
        assertThat(payment(fine).getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    // a payment the core refused: REFUND_PENDING until the job sends the money back
    private String refusedOrder(int userId) throws Exception {
        String driver = token(userId, "User");
        String order = order(userId, driver);
        CORE.stubFor(post(urlEqualTo("/internal/bookings")).willReturn(aResponse().withStatus(409)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"status\":409,\"message\":\"Car number 7 is already registered for this session\"}")));
        assertThat(status(confirm(driver, 7, order))).isEqualTo(409);
        assertThat(payment(order).getStatus()).isEqualTo(PaymentStatus.REFUND_PENDING);
        return order;
    }

    // checkout step 1: the core's check (stubbed for this driver), then the fake razorpay order
    private String order(int userId, String driver) throws Exception {
        CORE.stubFor(post(urlEqualTo("/internal/bookings/check")).willReturn(okJson("""
                {"sessionId": 1, "price": 650.0, "description": "Test Circuit 2026-11-01 10:00-11:00",
                 "waitlisted": false, "userId": %d, "driverName": "Driver %d", "driverEmail": "driver%d@gridpulse.test",
                 "driverMobile": "9000000%d", "trackName": "Test Circuit", "trackLocation": "Pune",
                 "sessionDate": "2026-11-01", "startTime": "10:00:00", "endTime": "11:00:00",
                 "sessionType": "PRACTICE", "phase": null, "competitionName": null}""".formatted(userId, userId,
                userId, userId))));
        ResponseEntity<String> order = send("/api/payments/order", driver, booking(7));
        assertThat(status(order)).isEqualTo(201);
        return json(order).get("orderId").asText();
    }

    // the job only looks at payments captured more than a minute ago
    private void capturedTwoMinutesAgo(String order) {
        Payment payment = payment(order);
        payment.setPaidAt(Instant.now().minus(2, ChronoUnit.MINUTES));
        paymentRepo.save(payment);
    }

    // the scheduled job is off in the tests, the test runs it once
    private RefundJob refundJob() {
        return new RefundJob(paymentRecordService, paymentAuditService, razorpayService, coreCalls);
    }
}
