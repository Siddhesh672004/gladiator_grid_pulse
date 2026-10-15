package com.gridpulse.payment.support;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.gridpulse.common.event.EventEnvelope;
import com.gridpulse.common.event.EventJson;
import com.gridpulse.common.event.EventReader;
import com.gridpulse.common.outbox.OutboxRepository;
import com.gridpulse.payment.model.Payment;
import com.gridpulse.payment.repository.PaymentRepo;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

// the real payment-service on a random port against a throw-away mysql in docker; wiremock plays the core,
// razorpay is faked. events stay in the outbox table (relay off, no kafka): the tests read them there
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "jwt.secret=" + PaymentIntegrationTest.JWT_SECRET,
        "gridpulse.internal-api-key=" + PaymentIntegrationTest.INTERNAL_KEY,
        "gridpulse.outbox.relay.enabled=false",
        "spring.kafka.admin.auto-create=false",
        "spring.kafka.listener.auto-startup=false",
        "razorpay.key-id=rzp_test_dummy",
        "razorpay.key-secret=dummy_secret",
        "gridpulse.jobs.refund.enabled=false",
        // no health checks against wiremock: the one listed core is always used
        "spring.cloud.loadbalancer.configurations=default",
        "gridpulse.log-dir=target/test-logs" })
@Import(FakeRazorpayService.class)
public abstract class PaymentIntegrationTest {

    protected static final String JWT_SECRET = "gridpulse-payment-tests-fixed-jwt-secret-0123456789abcdefghijklmn";
    protected static final String INTERNAL_KEY = "gridpulse-payment-tests-internal-key-0123456789";

    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0");

    protected static final WireMockServer CORE = new WireMockServer(WireMockConfiguration.options().dynamicPort());

    static {
        MYSQL.start();
        CORE.start();
    }

    @DynamicPropertySource
    static void coreOnWireMock(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.discovery.client.simple.instances.core-service[0].uri",
                () -> "http://localhost:" + CORE.port());
    }

    @Autowired
    protected TestRestTemplate rest;

    @Autowired
    protected PaymentRepo paymentRepo;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private EventReader eventReader;

    @Autowired
    private CircuitBreakerRegistry circuitBreakers;

    @BeforeEach
    void freshCore() {
        CORE.resetAll();
        circuitBreakers.circuitBreaker("core").reset();
    }

    // a token like the core's (same secret): user id + role are all payment-service reads
    protected static String token(int userId, String role) {
        return Jwts.builder()
                .claim("userId", userId)
                .claim("username", "Driver " + userId)
                .claim("userRole", role)
                .setSubject("driver" + userId + "@gridpulse.test")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(SignatureAlgorithm.HS256, JWT_SECRET.getBytes(StandardCharsets.UTF_8))
                .compact();
    }

    protected ResponseEntity<String> send(String url, String token, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Optional.ofNullable(token).ifPresent(headers::setBearerAuth);
        return rest.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
    }

    protected JsonNode json(ResponseEntity<String> response) throws JsonProcessingException {
        return objectMapper.readTree(response.getBody());
    }

    protected static int status(ResponseEntity<?> response) {
        return response.getStatusCode().value();
    }

    protected static Map<String, Object> booking(int carNumber) {
        return Map.of("sessionId", 1, "carNumber", carNumber, "carCategory", "GT3", "termsAccepted", true);
    }

    // checkout step 2 with the signature the fake accepts
    protected ResponseEntity<String> confirm(String token, int carNumber, String orderId) {
        return send("/api/payments/confirm", token, Map.of("booking", booking(carNumber), "razorpayOrderId", orderId,
                "razorpayPaymentId", "pay_" + orderId, "razorpaySignature", "valid"));
    }

    protected Payment payment(String orderId) {
        return paymentRepo.findAll().stream()
                .filter(payment -> payment.getRazorpayOrderId().equals(orderId))
                .findFirst()
                .orElseThrow();
    }

    // the statuses this order announced (PaymentStatusChanged in the outbox), oldest first
    protected List<String> announcedStatuses(String orderId) {
        return outboxRepository.findAll(Sort.by("id")).stream()
                .filter(row -> row.getType().equals("PaymentStatusChanged"))
                .map(row -> eventReader.read(row.getPayload()).data())
                .filter(data -> data.path("orderId").asText().equals(orderId))
                .map(data -> data.path("status").asText())
                .toList();
    }

    protected static String envelope(Object payload) throws JsonProcessingException {
        return EventJson.MAPPER.writeValueAsString(new EventEnvelope(UUID.randomUUID().toString(),
                payload.getClass().getSimpleName(), Instant.now(), "core", EventJson.MAPPER.valueToTree(payload)));
    }
}
