package com.gridpulse.payment.client;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gridpulse.payment.dto.request.RaceBookingRequest;
import com.gridpulse.payment.exceptions.CoreRefusedException;
import com.gridpulse.payment.exceptions.CoreUnavailableException;
import com.gridpulse.web.error.ErrorResponse;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// every call to the core: a 4xx is the core's answer (CoreRefusedException, same status + message),
// no answer / 5xx / open breaker = CoreUnavailableException. tried again (2x, 300 ms) only where that is safe
@Slf4j
@Component
@RequiredArgsConstructor
public class CoreCalls {

    private static final String NOT_ANSWERING = "Bookings cannot be checked right now. Please try again in a minute.";

    private final CoreClient coreClient;
    private final ObjectMapper objectMapper;

    // no retry: the driver is waiting and can simply click again
    @CircuitBreaker(name = "core", fallbackMethod = "checkBusy")
    public BookingQuote check(RaceBookingRequest request) {
        return call(() -> coreClient.check(request));
    }

    // safe to repeat: the core saves one booking per order
    @Retry(name = "core")
    @CircuitBreaker(name = "core", fallbackMethod = "createBookingBusy")
    public JsonNode createBooking(InternalBookingRequest request) {
        return call(() -> coreClient.createBooking(request));
    }

    // empty = the core has no booking for this order
    @Retry(name = "core")
    @CircuitBreaker(name = "core", fallbackMethod = "findByOrderBusy")
    public Optional<JsonNode> findByOrder(String orderId) {
        try {
            return Optional.of(call(() -> coreClient.findByOrder(orderId)));
        } catch (CoreRefusedException e) {
            if (e.getStatus() == HttpStatus.NOT_FOUND.value()) {
                return Optional.empty();
            }
            throw e;
        }
    }

    // breaker open (the core failed a lot just now): answer at once instead of waiting for a timeout
    @SuppressWarnings("unused")
    private BookingQuote checkBusy(RaceBookingRequest request, CallNotPermittedException e) {
        throw new CoreUnavailableException(NOT_ANSWERING, e);
    }

    @SuppressWarnings("unused")
    private JsonNode createBookingBusy(InternalBookingRequest request, CallNotPermittedException e) {
        throw new CoreUnavailableException(NOT_ANSWERING, e);
    }

    @SuppressWarnings("unused")
    private Optional<JsonNode> findByOrderBusy(String orderId, CallNotPermittedException e) {
        throw new CoreUnavailableException(NOT_ANSWERING, e);
    }

    private <T> T call(Supplier<T> request) {
        try {
            return request.get();
        } catch (HttpClientErrorException e) {
            throw refused(e);
        } catch (RestClientException | IllegalStateException | IllegalArgumentException e) {
            // 5xx, timeout, connection refused, or no healthy core instance (the load balancer throws an
            // IllegalArgument / IllegalStateException then)
            log.warn("The core did not answer: {}", e.getClass().getSimpleName());
            throw new CoreUnavailableException(NOT_ANSWERING, e);
        }
    }

    // the core's ErrorResponse: message + field errors as the core wrote them
    private CoreRefusedException refused(HttpClientErrorException e) {
        Optional<ErrorResponse> body = readError(e.getResponseBodyAsString());
        String message = body.map(ErrorResponse::getMessage).orElse(e.getStatusText());
        Map<String, String> fieldErrors = body.map(ErrorResponse::getFieldErrors).orElse(null);
        return new CoreRefusedException(e.getStatusCode().value(), message, fieldErrors);
    }

    private Optional<ErrorResponse> readError(String json) {
        try {
            return Optional.ofNullable(objectMapper.readValue(json, ErrorResponse.class));
        } catch (JsonProcessingException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
