package com.examly.springapp.support;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.MySQLContainer;

import com.examly.springapp.config.properties.EmailProperties;
import com.examly.springapp.dto.request.BookingStatusRequest;
import com.examly.springapp.dto.request.PaymentConfirmRequest;
import com.examly.springapp.dto.request.RaceBookingRequest;
import com.examly.springapp.model.BookingStatus;
import com.examly.springapp.model.RaceSession;
import com.examly.springapp.model.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

// the real app on a random port against a throw-away mysql in docker
// razorpay is faked, no mail / ai / cloudinary call leaves the machine, the refund job is off
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "MAIL_TO_FILE=true",
        "spring.mail.username=",
        "spring.mail.password=",
        "jwt.secret=" + IntegrationTest.JWT_SECRET,
        "gemini.api-key=",
        "gemini.api-key2=",
        "gemini.api-key3=",
        "groq.api-key=",
        "cloudinary.cloud-name=",
        "cloudinary.api-key=",
        "cloudinary.api-secret=",
        "razorpay.key-id=rzp_test_dummy",
        "razorpay.key-secret=dummy_secret",
        "gridpulse.super-admin.email=",
        "gridpulse.super-admin.password=",
        "gridpulse.jobs.refund.enabled=false",
        "gridpulse.log-dir=target/test-logs",
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "spring.cache.type=none" })
@Import({ FakeRazorpayService.class, TestData.class })
public abstract class IntegrationTest {

    static final String JWT_SECRET = "gridpulse-integration-tests-fixed-jwt-secret-0123456789abcdefghij";

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
    private EmailProperties emailProperties;

    protected ResponseEntity<String> get(String url, String token) {
        return call(HttpMethod.GET, url, token, null);
    }

    protected ResponseEntity<String> post(String url, String token, Object body) {
        return call(HttpMethod.POST, url, token, body);
    }

    protected ResponseEntity<String> put(String url, String token, Object body) {
        return call(HttpMethod.PUT, url, token, body);
    }

    private ResponseEntity<String> call(HttpMethod method, String url, String token, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Optional.ofNullable(token).ifPresent(headers::setBearerAuth);
        return rest.exchange(url, method, new HttpEntity<>(body, headers), String.class);
    }

    protected JsonNode json(ResponseEntity<String> response) throws JsonProcessingException {
        return objectMapper.readTree(response.getBody());
    }

    protected static int status(ResponseEntity<?> response) {
        return response.getStatusCode().value();
    }

    // checkout step 1, returns the (fake) razorpay order id
    protected String createOrder(User driver, RaceSession session, int carNumber) throws JsonProcessingException {
        ResponseEntity<String> order = post("/api/payments/order", data.token(driver), booking(session, carNumber));
        if (status(order) != 201) {
            throw new IllegalStateException("order failed: " + order.getBody());
        }
        return json(order).get("orderId").asText();
    }

    // checkout step 2 with the signature the fake accepts
    protected ResponseEntity<String> confirm(User driver, RaceSession session, int carNumber, String orderId) {
        PaymentConfirmRequest paid = new PaymentConfirmRequest(booking(session, carNumber), orderId,
                "pay_" + orderId, "valid");
        return post("/api/payments/confirm", data.token(driver), paid);
    }

    // order + confirm = a Pending booking, returns its id
    protected Long book(User driver, RaceSession session, int carNumber) throws JsonProcessingException {
        ResponseEntity<String> booked = confirm(driver, session, carNumber, createOrder(driver, session, carNumber));
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

    // texts of the test-mode email files sent to this address
    protected List<String> mailsTo(String email) throws IOException {
        Path folder = Path.of(emailProperties.fileDir());
        if (!Files.isDirectory(folder)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(folder)) {
            return files.filter(file -> file.getFileName().toString().endsWith("_" + email + ".txt"))
                    .map(IntegrationTest::read)
                    .toList();
        }
    }

    private static String read(Path file) {
        try {
            return Files.readString(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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
