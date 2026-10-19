package com.gridpulse.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import com.gridpulse.core.model.RaceSession;
import com.gridpulse.core.model.User;
import com.gridpulse.core.support.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.gridpulse.common.security.InternalApi;

// /internal/** is for payment-service: the key always, the driver's token too except for by-order
class InternalApiIntegrationTest extends IntegrationTest {

    @Test
    void keyAndTokenRules() throws Exception {
        RaceSession session = data.practiceSession(5);
        User driver = data.driver();
        Map<String, Object> booking = Map.of("sessionId", session.getSessionId(), "carNumber", 5,
                "carCategory", "GT3", "termsAccepted", true);

        ResponseEntity<String> noKey = post("/internal/bookings/check", data.token(driver), booking);
        assertThat(status(noKey)).isEqualTo(401);
        assertThat(json(noKey).get("message").asText()).isEqualTo("Missing or wrong internal key");
        assertThat(status(postInternal("/internal/bookings/check", null, booking))).isEqualTo(401);

        JsonNode quote = json(postInternal("/internal/bookings/check", data.token(driver), booking));
        assertThat(quote.get("price").asDouble()).isEqualTo(500.0);
        assertThat(quote.get("waitlisted").asBoolean()).isFalse();
        assertThat(quote.get("driverEmail").asText()).isEqualTo(driver.getEmail());
        // a wrong rule gives the booking page's answer
        Map<String, Object> badCar = Map.of("sessionId", session.getSessionId(), "carNumber", 5,
                "carCategory", "Tractor", "termsAccepted", true);
        assertThat(status(postInternal("/internal/bookings/check", data.token(driver), badCar))).isEqualTo(400);
    }

    @Test
    void byOrderNeedsOnlyTheKey() throws Exception {
        RaceSession session = data.practiceSession(5);
        String order = newOrderId();

        assertThat(status(byOrder(order, INTERNAL_KEY))).isEqualTo(404);
        assertThat(status(createPaid(data.driver(), session, 6, order))).isEqualTo(201);
        ResponseEntity<String> found = byOrder(order, INTERNAL_KEY);
        assertThat(status(found)).isEqualTo(200);
        assertThat(json(found).get("carNumber").asInt()).isEqualTo(6);
        assertThat(status(byOrder(order, "a-wrong-key-a-wrong-key-a-wrong-key"))).isEqualTo(401);
    }

    private ResponseEntity<String> byOrder(String order, String key) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(InternalApi.HEADER, key);
        return rest.exchange("/internal/bookings/by-order/" + order, HttpMethod.GET, new HttpEntity<>(headers),
                String.class);
    }
}
