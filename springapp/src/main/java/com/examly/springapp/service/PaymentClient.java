package com.examly.springapp.service;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.examly.springapp.config.properties.PaymentServiceProperties;
import com.examly.springapp.dto.response.ChartPoint;
import com.examly.springapp.dto.response.RevenueResponse;
import com.examly.springapp.repository.MonthAmount;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;

// payment-service, called with the admin's own token: the revenue of the admin dashboard
// payment-service down -> the dashboard shows 0 (warning in the log)
@Slf4j
@Component
public class PaymentClient {

    private final RestClient restClient;
    public PaymentClient(PaymentServiceProperties properties) {
        // a slow payment-service must not hold the dashboard for long
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build());
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder()
                .baseUrl(properties.paymentUrl())
                .requestFactory(factory)
                .requestInterceptor((request, body, execution) -> {
                    callerToken().ifPresent(token -> request.getHeaders().set(HttpHeaders.AUTHORIZATION, token));
                    return execution.execute(request, body);
                })
                .build();
    }

    @CircuitBreaker(name = "payment", fallbackMethod = "noSummary")
    public RevenueResponse summary() {
        return restClient.get().uri("/api/payments/summary").retrieve().body(RevenueResponse.class);
    }

    // no revenue numbers: the rest of the dashboard still shows
    private RevenueResponse noSummary(Exception e) {
        log.warn("Revenue from payment-service not available: {}", e.getClass().getSimpleName());
        List<ChartPoint> months = IntStream.range(0, 12)
                .mapToObj(month -> new ChartPoint(MonthAmount.MONTHS[month], 0))
                .toList();
        return new RevenueResponse(0, 0, 0, 0, months);
    }

    // the Authorization header of the request being served
    private static Optional<String> callerToken() {
        return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                .filter(ServletRequestAttributes.class::isInstance)
                .map(attributes -> ((ServletRequestAttributes) attributes).getRequest()
                        .getHeader(HttpHeaders.AUTHORIZATION));
    }
}
