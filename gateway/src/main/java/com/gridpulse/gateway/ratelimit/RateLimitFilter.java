package com.gridpulse.gateway.ratelimit;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.PathContainer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import reactor.core.publisher.Mono;

// runs before everything else: one token bucket per rule and client (user id, or the address)
// an empty bucket -> 429 with Retry-After, the request never reaches a core instance
@Component
public class RateLimitFilter implements GlobalFilter, Ordered {

    // a rule ready to use: its place in the list (part of the bucket key) and its parsed paths
    private record LoadedRule(int index, HttpMethod method, List<PathPattern> paths, RateLimitProperties.Rule rule) {

        boolean matches(HttpMethod requestMethod, PathContainer path) {
            return (method == null || method.equals(requestMethod))
                    && paths.stream().anyMatch(pattern -> pattern.matches(path));
        }
    }

    private final List<LoadedRule> rules;
    private final TokenUsers tokenUsers;
    private final ObjectMapper objectMapper;
    private final boolean trustProxy;
    // a quiet client's buckets are forgotten only after the longest rule period, so a daily cap stays daily
    private final Cache<String, Bucket> buckets;

    public RateLimitFilter(RateLimitProperties properties, TokenUsers tokenUsers, ObjectMapper objectMapper,
            @Value("${gridpulse.trust-proxy:false}") boolean trustProxy) {
        List<RateLimitProperties.Rule> configured = Optional.ofNullable(properties.rules()).orElse(List.of());
        this.rules = IntStream.range(0, configured.size())
                .mapToObj(i -> load(i, configured.get(i)))
                .toList();
        this.tokenUsers = tokenUsers;
        this.objectMapper = objectMapper;
        this.trustProxy = trustProxy;
        this.buckets = Caffeine.newBuilder()
                .expireAfterAccess(longestPeriod(configured))
                .maximumSize(100_000)
                .build();
    }

    private static Duration longestPeriod(List<RateLimitProperties.Rule> rules) {
        return rules.stream()
                .map(RateLimitProperties.Rule::period)
                .filter(Objects::nonNull)
                .reduce(Duration.ofMinutes(15), (a, b) -> a.compareTo(b) >= 0 ? a : b);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        // a CORS preflight is never counted
        if (HttpMethod.OPTIONS.equals(request.getMethod())) {
            return chain.filter(exchange);
        }
        PathContainer path = request.getPath().pathWithinApplication();
        return rules.stream()
                .filter(rule -> rule.matches(request.getMethod(), path))
                .findFirst()
                .filter(rule -> rule.rule().capacity() > 0)
                .map(rule -> bucketOf(rule, request).tryConsumeAndReturnRemaining(1))
                .filter(probe -> !probe.isConsumed())
                .map(probe -> tooManyRequests(exchange, probe))
                .orElseGet(() -> chain.filter(exchange));
    }

    private Bucket bucketOf(LoadedRule rule, ServerHttpRequest request) {
        String client = clientKey(rule.rule().key(), request);
        return buckets.get(rule.index() + "|" + client, key -> Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(rule.rule().capacity())
                        .refillGreedy(rule.rule().capacity(), rule.rule().period())
                        .build())
                .build());
    }

    // u:<userId> for a valid token (CLIENT rules), else ip:<address>
    private String clientKey(KeyType type, ServerHttpRequest request) {
        return Optional.ofNullable(type)
                .filter(KeyType.CLIENT::equals)
                .flatMap(client -> tokenUsers.userId(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION)))
                .map(userId -> "u:" + userId)
                .orElseGet(() -> "ip:" + clientAddress(request));
    }

    // behind a trusted proxy the visitor is the first X-Forwarded-For entry; anyone else could fake that header
    private String clientAddress(ServerHttpRequest request) {
        return Optional.of(trustProxy)
                .filter(Boolean::booleanValue)
                .map(trusted -> request.getHeaders().getFirst("X-Forwarded-For"))
                .map(forwarded -> forwarded.split(",")[0].trim())
                .filter(first -> !first.isEmpty())
                .orElseGet(() -> Optional.ofNullable(request.getRemoteAddress())
                        .map(InetSocketAddress::getHostString)
                        .orElse("unknown"));
    }

    // the same json as every core error (ErrorResponse), so angular shows err.error.message
    private Mono<Void> tooManyRequests(ServerWebExchange exchange, ConsumptionProbe probe) {
        // whole seconds, rounded up: never "try again in 0 seconds"
        long seconds = Math.max(1, (probe.getNanosToWaitForRefill() + 999_999_999) / 1_000_000_000);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", HttpStatus.TOO_MANY_REQUESTS.value());
        body.put("error", HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase());
        body.put("message", "Too many requests. Please try again in " + seconds + " seconds.");
        body.put("path", exchange.getRequest().getPath().value());

        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().set(HttpHeaders.RETRY_AFTER, String.valueOf(seconds));
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(json(body))));
    }

    private byte[] json(Map<String, Object> body) {
        try {
            return objectMapper.writeValueAsBytes(body);
        } catch (JsonProcessingException e) {
            return String.valueOf(body.get("message")).getBytes(StandardCharsets.UTF_8);
        }
    }

    private static LoadedRule load(int index, RateLimitProperties.Rule rule) {
        HttpMethod method = Optional.ofNullable(rule.method())
                .filter(text -> !text.isBlank())
                .map(HttpMethod::valueOf)
                .orElse(null);
        List<PathPattern> paths = rule.paths().stream()
                .map(PathPatternParser.defaultInstance::parse)
                .toList();
        return new LoadedRule(index, method, paths, rule);
    }
}
