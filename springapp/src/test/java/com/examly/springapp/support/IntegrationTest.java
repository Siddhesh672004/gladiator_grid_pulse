package com.examly.springapp.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.MySQLContainer;

import com.examly.springapp.dto.request.BookingStatusRequest;
import com.examly.springapp.dto.request.InternalBookingRequest;
import com.examly.springapp.dto.request.RaceBookingRequest;
import com.examly.springapp.model.BookingStatus;
import com.examly.springapp.model.RaceSession;
import com.examly.springapp.model.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gridpulse.common.event.EventReader;
import com.gridpulse.common.outbox.OutboxRepository;
import com.gridpulse.common.security.InternalApi;

// the real app on a random port against a throw-away mysql in docker
// bookings are made the way payment-service makes them (/internal/bookings), no ai / cloudinary call leaves the machine
// events stay in the outbox table (relay off, no kafka needed): the tests read them there
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "jwt.secret=" + IntegrationTest.JWT_SECRET,
        "gridpulse.ticket-secret=" + IntegrationTest.TICKET_SECRET,
        "gridpulse.outbox.relay.enabled=false",
        "spring.kafka.admin.auto-create=false",
        "spring.kafka.listener.auto-startup=false",
        "groq.api-key=",
        "cloudinary.cloud-name=",
        "cloudinary.api-key=",
        "cloudinary.api-secret=",
        "gridpulse.internal-api-key=" + IntegrationTest.INTERNAL_KEY,
        "gridpulse.payment-url=http://localhost:1",
        "gridpulse.super-admin.email=",
        "gridpulse.super-admin.password=",
        "gridpulse.log-dir=target/test-logs",
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "spring.cache.type=none" })
@Import(TestData.class)
public abstract class IntegrationTest {

    static final String JWT_SECRET = "gridpulse-integration-tests-fixed-jwt-secret-0123456789abcdefghij";
    protected static final String TICKET_SECRET = "gridpulse-integration-tests-ticket-secret-0123456789";
    protected static final String INTERNAL_KEY = "gridpulse-integration-tests-internal-key-0123456789";
    // one counter for the whole run: every booking gets its own razorpay order id
    private static final AtomicInteger ORDERS = new AtomicInteger();

    // one container for every test class (same spring context), docker removes it when the jvm stops
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0");

    static {
        MYSQL.start();
    }

    @Autowired
    protected TestRestTemplate rest;

    @Autowired
    protected TestData data;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private EventReader eventReader;

    protected ResponseEntity<String> get(String url, String token) {
        return call(HttpMethod.GET, url, token, null);
    }

    protected ResponseEntity<String> post(String url, String token, Object body) {
        return call(HttpMethod.POST, url, token, body);
    }

    protected ResponseEntity<String> put(String url, String token, Object body) {
        return call(HttpMethod.PUT, url, token, body);
    }

    protected ResponseEntity<String> delete(String url, String token) {
        return call(HttpMethod.DELETE, url, token, null);
    }

    // what payment-service sends: the internal key + the driver's own token
    protected ResponseEntity<String> postInternal(String url, String token, Object body) {
        HttpHeaders headers = headers(token);
        headers.set(InternalApi.HEADER, INTERNAL_KEY);
        return rest.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
    }

    private ResponseEntity<String> call(HttpMethod method, String url, String token, Object body) {
        return rest.exchange(url, method, new HttpEntity<>(body, headers(token)), String.class);
    }

    private static HttpHeaders headers(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Optional.ofNullable(token).ifPresent(headers::setBearerAuth);
        return headers;
    }

    protected JsonNode json(ResponseEntity<String> response) throws JsonProcessingException {
        return objectMapper.readTree(response.getBody());
    }

    protected static int status(ResponseEntity<?> response) {
        return response.getStatusCode().value();
    }

    protected static String newOrderId() {
        return "order_test_" + ORDERS.incrementAndGet();
    }

    // payment-service after the money came in: the booking of this order
    protected ResponseEntity<String> createPaid(User driver, RaceSession session, int carNumber, String orderId) {
        InternalBookingRequest paid = new InternalBookingRequest(booking(session, carNumber), orderId,
                "pay_" + orderId, data.priceOf(session));
        return postInternal("/internal/bookings", data.token(driver), paid);
    }

    // a paid Pending booking, returns its id
    protected Long book(User driver, RaceSession session, int carNumber) throws JsonProcessingException {
        ResponseEntity<String> booked = createPaid(driver, session, carNumber, newOrderId());
        if (status(booked) != 201) {
            throw new IllegalStateException("booking failed: " + booked.getBody());
        }
        return json(booked).get("raceBookingId").asLong();
    }

    protected ResponseEntity<String> approve(String adminToken, Long bookingId) {
        return put("/api/race-booking/" + bookingId, adminToken, new BookingStatusRequest(BookingStatus.APPROVED, null));
    }

    private static RaceBookingRequest booking(RaceSession session, int carNumber) {
        return new RaceBookingRequest(session.getSessionId(), carNumber, "GT3", null, true);
    }

    // the payloads of the outbox rows of one event type, oldest first
    protected <T> List<T> outboxEvents(Class<T> type) {
        return outboxRepository.findAll(Sort.by("id")).stream()
                .filter(row -> row.getType().equals(type.getSimpleName()))
                .map(row -> eventReader.payload(eventReader.read(row.getPayload()), type))
                .toList();
    }

    // a file download (the e-ticket pdf)
    protected ResponseEntity<byte[]> download(String url, String token) {
        HttpHeaders headers = new HttpHeaders();
        Optional.ofNullable(token).ifPresent(headers::setBearerAuth);
        return rest.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), byte[].class);
    }

    // every call waits at the gate, then all go at once; answers come back in the same order
    @SafeVarargs
    protected static List<ResponseEntity<String>> atTheSameTime(Supplier<ResponseEntity<String>>... calls)
            throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(calls.length);
        CountDownLatch ready = new CountDownLatch(calls.length);
        CountDownLatch go = new CountDownLatch(1);
        try {
            List<Future<ResponseEntity<String>>> running = new ArrayList<>();
            for (Supplier<ResponseEntity<String>> call : calls) {
                running.add(pool.submit(() -> {
                    ready.countDown();
                    go.await();
                    return call.get();
                }));
            }
            ready.await();
            go.countDown();
            List<ResponseEntity<String>> answers = new ArrayList<>();
            for (Future<ResponseEntity<String>> answer : running) {
                answers.add(answer.get(60, TimeUnit.SECONDS));
            }
            return answers;
        } finally {
            pool.shutdownNow();
        }
    }
}
