package com.examly.springapp.service;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.examly.springapp.config.properties.PaymentServiceProperties;
import com.examly.springapp.dto.request.RaceBookingRequest;
import com.examly.springapp.dto.response.ChartPoint;
import com.examly.springapp.dto.response.ErrorResponse;
import com.examly.springapp.dto.response.PaymentOrderResponse;
import com.examly.springapp.dto.response.RevenueResponse;
import com.examly.springapp.exceptions.BookingException;
import com.examly.springapp.exceptions.ConflictException;
import com.examly.springapp.exceptions.GatewayException;
import com.examly.springapp.exceptions.GridPulseException;
import com.examly.springapp.repository.MonthAmount;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;

// payment-service, called with the caller's own token: the revenue of the admin dashboard and the chat's order
// payment-service down -> the dashboard shows 0 (warning in the log), the chat says payments can't be reached
@Slf4j
@Component
public class PaymentClient {

    private static final String NOT_REACHED = "Payments cannot be reached right now. Please try again in a minute.";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public PaymentClient(PaymentServiceProperties properties, ObjectMapper objectMapper) {
        // an order waits for the core's check and razorpay, so 10 s to answer
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build());
        factory.setReadTimeout(Duration.ofSeconds(10));
        this.restClient = RestClient.builder()
                .baseUrl(properties.paymentUrl())
                .requestFactory(factory)
                .requestInterceptor((request, body, execution) -> {
                    callerToken().ifPresent(token -> request.getHeaders().set(HttpHeaders.AUTHORIZATION, token));
                    return execution.execute(request, body);
                })
                .build();
        this.objectMapper = objectMapper;
    }

    @CircuitBreaker(name = "payment", fallbackMethod = "noSummary")
    public RevenueResponse summary() {
        return restClient.get().uri("/api/payments/summary").retrieve().body(RevenueResponse.class);
    }

    // the chat's BookingTool: the same order as "Pay & Register"; a refused booking keeps payment-service's message
    @CircuitBreaker(name = "payment", fallbackMethod = "orderBusy")
    public PaymentOrderResponse createOrder(RaceBookingRequest request) {
        try {
            return restClient.post().uri("/api/payments/order").body(request).retrieve()
                    .body(PaymentOrderResponse.class);
        } catch (HttpClientErrorException e) {
            throw refused(e);
        } catch (RestClientException e) {
            log.warn("payment-service did not answer the order: {}", e.getClass().getSimpleName());
            throw new GatewayException(NOT_REACHED, e);
        }
    }

    // no revenue numbers: the rest of the dashboard still shows
    private RevenueResponse noSummary(Exception e) {
        log.warn("Revenue from payment-service not available: {}", e.getClass().getSimpleName());
        List<ChartPoint> months = IntStream.range(0, 12)
                .mapToObj(month -> new ChartPoint(MonthAmount.MONTHS[month], 0))
                .toList();
        return new RevenueResponse(0, 0, 0, 0, months);
    }

    private PaymentOrderResponse orderBusy(RaceBookingRequest request, CallNotPermittedException e) {
        throw new GatewayException(NOT_REACHED, e);
    }

    // 409 stays a conflict, any other 4xx is a booking that is not possible
    private GridPulseException refused(HttpClientErrorException e) {
        String message = readMessage(e.getResponseBodyAsString()).orElse(e.getStatusText());
        return e.getStatusCode().value() == HttpStatus.CONFLICT.value()
                ? new ConflictException(message)
                : new BookingException(message);
    }

    private Optional<String> readMessage(String json) {
        try {
            return Optional.ofNullable(objectMapper.readValue(json, ErrorResponse.class).getMessage());
        } catch (JsonProcessingException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    // the Authorization header of the request being served
    private static Optional<String> callerToken() {
        return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                .filter(ServletRequestAttributes.class::isInstance)
                .map(attributes -> ((ServletRequestAttributes) attributes).getRequest()
                        .getHeader(HttpHeaders.AUTHORIZATION));
    }
}
