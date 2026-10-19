package com.gridpulse.ai.config;

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Optional;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gridpulse.ai.client.CoreClient;
import com.gridpulse.ai.client.PaymentClient;
import com.gridpulse.ai.exceptions.ServiceDownException;
import com.gridpulse.ai.exceptions.ToolCallException;
import com.gridpulse.common.security.InternalApi;
import com.gridpulse.web.error.ErrorResponse;
import com.gridpulse.web.security.InternalApiProperties;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;

// the core and payment-service behind load balanced RestClients (http://core-service, http://payment-service)
// every call carries the caller's own token (the services check the user themselves), /internal/** also the key;
// one circuit breaker per service counts no answer / 5xx. a 4xx is the service's answer -> ToolCallException
@Slf4j
@Configuration
public class ClientConfig {

    @Bean
    @LoadBalanced
    public RestClient.Builder loadBalancedRestClientBuilder() {
        return RestClient.builder();
    }

    // the load balancer's health checks call every instance directly with this one
    @Bean
    public RestClient healthCheckRestClient() {
        return RestClient.builder().requestFactory(requestFactory()).build();
    }

    @Bean
    public CoreClient coreClient(RestClient.Builder loadBalancedRestClientBuilder, InternalApiProperties internal,
            CircuitBreakerRegistry breakers, ObjectMapper objectMapper) {
        return client(loadBalancedRestClientBuilder, "http://core-service", breakers.circuitBreaker("core"), internal,
                objectMapper, CoreClient.class);
    }

    @Bean
    public PaymentClient paymentClient(RestClient.Builder loadBalancedRestClientBuilder, InternalApiProperties internal,
            CircuitBreakerRegistry breakers, ObjectMapper objectMapper) {
        return client(loadBalancedRestClientBuilder, "http://payment-service", breakers.circuitBreaker("payment"),
                internal, objectMapper, PaymentClient.class);
    }

    static <T> T client(RestClient.Builder builder, String baseUrl, CircuitBreaker breaker,
            InternalApiProperties internal, ObjectMapper objectMapper, Class<T> type) {
        String key = InternalApi.keyOrDevDefault(internal.internalApiKey());
        RestClient restClient = builder.clone()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory())
                .requestInterceptor(headers(key))
                .requestInterceptor(breaker(breaker))
                .defaultStatusHandler(status -> status.is4xxClientError(),
                        (request, response) -> {
                            throw refused(response, objectMapper);
                        })
                .build();
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient)).build().createClient(type);
    }

    // the caller's token, and the internal key only where it is needed
    private static ClientHttpRequestInterceptor headers(String key) {
        return (request, body, execution) -> {
            callerToken().ifPresent(token -> request.getHeaders().set(HttpHeaders.AUTHORIZATION, token));
            Optional.of(request.getURI().getPath())
                    .filter(path -> path.startsWith("/internal/"))
                    .ifPresent(path -> request.getHeaders().set(InternalApi.HEADER, key));
            return execution.execute(request, body);
        };
    }

    // no answer and 5xx count as failures; open breaker = CallNotPermittedException at once
    private static ClientHttpRequestInterceptor breaker(CircuitBreaker breaker) {
        return (request, body, execution) -> {
            try {
                return breaker.executeCheckedSupplier(() -> {
                    ClientHttpResponse response = execution.execute(request, body);
                    if (response.getStatusCode().is5xxServerError()) {
                        int status = response.getStatusCode().value();
                        response.close();
                        throw new ServiceDownException(breaker.getName() + " answered " + status);
                    }
                    return response;
                });
            } catch (IOException | RuntimeException e) {
                throw e;
            } catch (Throwable e) {
                throw new IllegalStateException(e);
            }
        };
    }

    private static ToolCallException refused(ClientHttpResponse response, ObjectMapper objectMapper)
            throws IOException {
        int status = response.getStatusCode().value();
        String message;
        try {
            message = Optional.ofNullable(objectMapper.readValue(response.getBody(), ErrorResponse.class).getMessage())
                    .orElse("Request refused (" + status + ")");
        } catch (IOException e) {
            message = "Request refused (" + status + ")";
        }
        return new ToolCallException(status, message);
    }

    // connect 2 s, answer 10 s (an order waits for razorpay)
    private static ClientHttpRequestFactory requestFactory() {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build());
        factory.setReadTimeout(Duration.ofSeconds(10));
        return factory;
    }

    // the Authorization header of the request being served
    private static Optional<String> callerToken() {
        return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                .filter(ServletRequestAttributes.class::isInstance)
                .map(attributes -> ((ServletRequestAttributes) attributes).getRequest()
                        .getHeader(HttpHeaders.AUTHORIZATION));
    }
}
