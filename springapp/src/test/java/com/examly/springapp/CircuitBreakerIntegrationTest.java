package com.examly.springapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

import com.examly.springapp.exceptions.GatewayException;
import com.examly.springapp.service.CloudinaryService;
import com.examly.springapp.support.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

// the real CloudinaryServiceImpl: the tests have no cloudinary keys, so every upload fails like a dead service
class CircuitBreakerIntegrationTest extends IntegrationTest {

    @Autowired
    private CloudinaryService cloudinaryService;

    @Autowired
    private CircuitBreakerRegistry circuitBreakers;

    @AfterEach
    void closeBreaker() {
        circuitBreakers.circuitBreaker("cloudinary").reset();
    }

    @Test
    void breakerOpensAfterTenFailures() {
        MockMultipartFile picture = new MockMultipartFile("file", "car.png", "image/png", new byte[] { 1, 2, 3 });
        for (int call = 0; call < 10; call++) {
            assertThatThrownBy(() -> cloudinaryService.upload(picture, "tracks"))
                    .isInstanceOf(GatewayException.class)
                    .hasMessageContaining("not set up");
        }

        assertThat(circuitBreakers.circuitBreaker("cloudinary").getState()).isEqualTo(CircuitBreaker.State.OPEN);
        // open: answered at once, cloudinary is not asked
        assertThatThrownBy(() -> cloudinaryService.upload(picture, "tracks"))
                .isInstanceOf(GatewayException.class)
                .hasMessageContaining("busy");
    }

    @Test
    void badPicturesKeepTheBreakerClosed() {
        MockMultipartFile notAPicture = new MockMultipartFile("file", "notes.txt", "text/plain", new byte[] { 1 });
        for (int call = 0; call < 12; call++) {
            assertThatThrownBy(() -> cloudinaryService.upload(notAPicture, "tracks"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        assertThat(circuitBreakers.circuitBreaker("cloudinary").getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    void adminsSeeTheBreakers() throws Exception {
        JsonNode breakers = json(get("/actuator/circuitbreakers", data.token(data.admin()))).get("circuitBreakers");

        assertThat(breakers.has("razorpay") && breakers.has("cloudinary") && breakers.has("groq")
                && breakers.has("gemini")).isTrue();
        assertThat(status(get("/actuator/circuitbreakers", data.token(data.driver())))).isEqualTo(403);
    }
}
