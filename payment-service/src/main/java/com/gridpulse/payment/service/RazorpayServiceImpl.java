package com.gridpulse.payment.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.stream.StreamSupport;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Service;

import com.gridpulse.payment.config.properties.RazorpayProperties;
import com.gridpulse.payment.exceptions.GatewayException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// plain HttpClient against Razorpay's REST api. the secret is never logged or sent to the browser
// no @Transactional on purpose: slow calls must not run while one of our rows is locked
@Slf4j
@Service
@RequiredArgsConstructor
public class RazorpayServiceImpl implements RazorpayService {

    private static final String NOT_REACHED = "The payment service cannot be reached right now. Please try again later";
    private static final String BUSY = "The payment service is busy. Please try again in a minute.";

    private final RazorpayProperties razorpay;
    private final ObjectMapper objectMapper;

    private HttpClient httpClient;

    @PostConstruct
    public void init() {
        httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(razorpay.timeoutSeconds())).build();
        // no keys: the server still starts, the log says why nobody can pay
        Optional.of(razorpay)
                .filter(keys -> !keys.isConfigured())
                .ifPresent(keys -> log.warn("RAZORPAY_KEY_ID / RAZORPAY_KEY_SECRET are not set in secrets.properties"
                        + " - payments cannot be made"));
    }

    @Override
    public boolean isConfigured() {
        return razorpay.isConfigured();
    }

    @Override
    public String keyId() {
        return razorpay.keyId();
    }

    @Override
    @CircuitBreaker(name = "razorpay", fallbackMethod = "createOrderBusy")
    public String createOrder(long amountPaise, String receipt) {
        Map<String, Object> body = Map.of("amount", amountPaise, "currency", "INR", "receipt", receipt);
        return send("POST", "/orders", body).path("id").asText();
    }

    // razorpay's rule: HMAC-SHA256(orderId|paymentId, key secret) as hex
    @Override
    public boolean isValidSignature(String orderId, String paymentId, String signature) {
        // without keys nothing can be checked, so nothing is valid
        return Optional.of(razorpay)
                .filter(RazorpayProperties::isConfigured)
                .map(keys -> hmacSha256(orderId + "|" + paymentId))
                .filter(expected -> MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                        signature.getBytes(StandardCharsets.UTF_8)))
                .isPresent();
    }

    @Override
    @CircuitBreaker(name = "razorpay", fallbackMethod = "refundBusy")
    public String refund(String paymentId, long amountPaise) {
        return send("POST", "/payments/" + paymentId + "/refund", Map.of("amount", amountPaise)).path("id").asText();
    }

    // "captured" = the money really reached us. only reads, so a failed try is asked again (RefundJob)
    @Override
    @Retry(name = "razorpayCheck")
    @CircuitBreaker(name = "razorpay", fallbackMethod = "findCapturedPaymentBusy")
    public String findCapturedPayment(String orderId) {
        JsonNode payments = send("GET", "/orders/" + orderId + "/payments", null).path("items");
        // the first captured payment of the order, null = nobody paid it
        return StreamSupport.stream(payments.spliterator(), false)
                .filter(payment -> "captured".equals(payment.path("status").asText()))
                .map(payment -> payment.path("id").asText())
                .findFirst()
                .orElse(null);
    }

    // breaker open (razorpay failed a lot just now): answer at once instead of waiting for a timeout
    @SuppressWarnings("unused")
    private String createOrderBusy(long amountPaise, String receipt, CallNotPermittedException e) {
        throw new GatewayException(BUSY);
    }

    @SuppressWarnings("unused")
    private String refundBusy(String paymentId, long amountPaise, CallNotPermittedException e) {
        throw new GatewayException(BUSY);
    }

    @SuppressWarnings("unused")
    private String findCapturedPaymentBusy(String orderId, CallNotPermittedException e) {
        throw new GatewayException(BUSY);
    }

    private JsonNode send(String method, String path, Map<String, Object> body) {
        // no keys in secrets.properties -> 503
        Optional.of(razorpay)
                .filter(RazorpayProperties::isConfigured)
                .orElseThrow(() -> new GatewayException("Payments are not set up yet. Please try again later"));
        try {
            HttpRequest.BodyPublisher json = (body == null)
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(razorpay.baseUrl() + path))
                    .timeout(Duration.ofSeconds(razorpay.timeoutSeconds()))
                    .header("Authorization", "Basic " + basicLogin())
                    .header("Content-Type", "application/json")
                    .method(method, json)
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode answer = objectMapper.readTree(response.body());
            // anything but 2xx is a refusal with razorpay's own reason
            return Optional.of(answer)
                    .filter(ok -> response.statusCode() / 100 == 2)
                    .orElseThrow(() -> refused(method, path, response.statusCode(), answer));
        } catch (IOException e) {
            log.warn("Razorpay could not be reached: {}", e.getMessage());
            throw new GatewayException(NOT_REACHED, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GatewayException(NOT_REACHED, e);
        }
    }

    private GatewayException refused(String method, String path, int status, JsonNode answer) {
        String reason = answer.path("error").path("description").asText("HTTP " + status);
        log.warn("Razorpay {} {} answered HTTP {}: {}", method, path, status, reason);
        return new GatewayException("Razorpay: " + reason);
    }

    private String basicLogin() {
        String login = razorpay.keyId() + ":" + razorpay.keySecret();
        return Base64.getEncoder().encodeToString(login.getBytes(StandardCharsets.UTF_8));
    }

    private String hmacSha256(String text) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(razorpay.keySecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException(e);
        }
    }
}
