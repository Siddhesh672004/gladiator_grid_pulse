package com.gridpulse.ai.support;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.testcontainers.containers.MySQLContainer;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.gridpulse.web.security.TokenUser;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

// the real ai-service on a random port against a throw-away mysql in docker; wiremock plays the core and
// payment-service. no model is ever called: no gemini / groq keys, the tests call tools and the Confirm api
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "jwt.secret=" + AiIntegrationTest.JWT_SECRET,
        "gridpulse.internal-api-key=" + AiIntegrationTest.INTERNAL_KEY,
        "gridpulse.outbox.relay.enabled=false",
        "spring.kafka.admin.auto-create=false",
        "spring.kafka.listener.auto-startup=false",
        "gemini.api-key=",
        "gemini.api-key2=",
        "gemini.api-key3=",
        "groq.api-key=",
        "spring.cache.type=none",
        // no health checks against wiremock: the one listed instance is always used
        "spring.cloud.loadbalancer.configurations=default",
        "gridpulse.log-dir=target/test-logs" })
public abstract class AiIntegrationTest {

    protected static final String JWT_SECRET = "gridpulse-ai-tests-fixed-jwt-secret-0123456789abcdefghijklmnopqrst";
    protected static final String INTERNAL_KEY = "gridpulse-ai-tests-internal-key-0123456789";

    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0");

    // the core and payment-service both: their urls do not overlap
    protected static final WireMockServer SERVICES = new WireMockServer(WireMockConfiguration.options().dynamicPort());

    static {
        MYSQL.start();
        SERVICES.start();
    }

    @DynamicPropertySource
    static void servicesOnWireMock(DynamicPropertyRegistry registry) {
        String url = "http://localhost:" + SERVICES.port();
        registry.add("spring.cloud.discovery.client.simple.instances.core-service[0].uri", () -> url);
        registry.add("spring.cloud.discovery.client.simple.instances.payment-service[0].uri", () -> url);
    }

    @Autowired
    protected TestRestTemplate rest;

    @BeforeEach
    void freshServices() {
        SERVICES.resetAll();
    }

    @AfterEach
    void forgetCaller() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    // a token like the core's (same secret): user id + role are all ai-service reads
    protected static String token(int userId, String role) {
        return Jwts.builder()
                .claim("userId", userId)
                .claim("username", role + " " + userId)
                .claim("userRole", role)
                .setSubject("user" + userId + "@gridpulse.test")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(SignatureAlgorithm.HS256, JWT_SECRET.getBytes(StandardCharsets.UTF_8))
                .compact();
    }

    // what JwtTokenFilter does for a request: the tools can be called directly as this user
    protected static String actAs(int userId, String role) {
        String token = token(userId, role);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new TokenUser(userId, "user" + userId + "@gridpulse.test", role + " " + userId, role), null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        return token;
    }

    protected ResponseEntity<String> post(String url, String token, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Optional.ofNullable(token).ifPresent(headers::setBearerAuth);
        return rest.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
    }

    protected static int status(ResponseEntity<?> response) {
        return response.getStatusCode().value();
    }
}
