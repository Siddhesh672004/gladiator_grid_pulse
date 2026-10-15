package com.gridpulse.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.sun.net.httpserver.HttpServer;

// the gateway against a stub core (a tiny http server that answers 200 {} to everything); no docker
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "gridpulse.trust-proxy=true",
        "gridpulse.jwt-secret=" })
@AutoConfigureWebTestClient
class GatewayIntegrationTest {

    private static final HttpServer CORE = startStubCore();

    @Autowired
    private WebTestClient client;

    @DynamicPropertySource
    static void core(DynamicPropertyRegistry registry) {
        String url = "http://localhost:" + CORE.getAddress().getPort();
        registry.add("CORE_1_URL", () -> url);
        registry.add("CORE_PRIMARY_URL", () -> url);
    }

    @AfterAll
    static void stopCore() {
        CORE.stop(0);
    }

    @Test
    void sixthLoginInAMinuteIsRefused() {
        for (int login = 1; login <= 5; login++) {
            assertThat(login("10.0.0.1").returnResult(String.class).getStatus().value()).isNotEqualTo(429);
        }

        login("10.0.0.1")
                .expectStatus().isEqualTo(429)
                .expectHeader().exists(HttpHeaders.RETRY_AFTER)
                .expectBody()
                .jsonPath("$.status").isEqualTo(429)
                .jsonPath("$.error").isEqualTo("Too Many Requests")
                .jsonPath("$.message").value(message -> assertThat((String) message)
                        .matches("Too many requests\\. Please try again in \\d+ seconds\\."))
                .jsonPath("$.path").isEqualTo("/api/login");
        // another address has its own bucket
        assertThat(login("10.0.0.2").returnResult(String.class).getStatus().value()).isNotEqualTo(429);
    }

    @Test
    void routesAndTheInternalBlock() {
        client.get().uri("/api/faqs").exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("{}");
        client.get().uri("/internal/anything").exchange()
                .expectStatus().isForbidden();
    }

    private WebTestClient.ResponseSpec login(String address) {
        return client.post().uri("/api/login")
                .header("X-Forwarded-For", address)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", "a@b.co", "password", "x"))
                .exchange();
    }

    private static HttpServer startStubCore() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/", exchange -> {
                byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
                exchange.getRequestBody().readAllBytes();
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(body);
                }
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
