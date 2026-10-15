package com.examly.springapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.kafka.KafkaContainer;

import com.examly.springapp.model.RaceSession;
import com.examly.springapp.support.IntegrationTest;
import com.gridpulse.common.event.Topics;
import com.gridpulse.common.outbox.OutboxEvent;
import com.gridpulse.common.outbox.OutboxRepository;

// with a real kafka (its own container): the outbox relay sends, and the live board hears the change through kafka
class KafkaIntegrationTest extends IntegrationTest {

    @ServiceConnection
    static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka:4.0.0");

    static {
        KAFKA.start();
    }

    // the other tests keep the relay and the kafka listener off
    @DynamicPropertySource
    static void kafkaOn(DynamicPropertyRegistry registry) {
        registry.add("gridpulse.outbox.relay.enabled", () -> "true");
        registry.add("spring.kafka.listener.auto-startup", () -> "true");
    }

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private KafkaListenerEndpointRegistry listeners;

    @Test
    void relaySendsTheApprovalToKafka() throws Exception {
        RaceSession session = data.practiceSession(5);
        Long bookingId = book(data.driver(), session, 61);
        approve(data.token(data.admin()), bookingId);
        OutboxEvent row = outboxRepository.findAll().stream()
                .filter(event -> event.getType().equals("BookingDecided"))
                .filter(event -> String.valueOf(bookingId).equals(event.getMsgKey()))
                .findFirst()
                .orElseThrow();

        await().atMost(Duration.ofSeconds(20))
                .until(() -> outboxRepository.findById(row.getId()).orElseThrow().getSentAt() != null);

        List<String> received = new ArrayList<>();
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "test-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"), new StringDeserializer(), new StringDeserializer())) {
            consumer.subscribe(List.of(Topics.BOOKING));
            await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
                consumer.poll(Duration.ofMillis(500)).forEach(message -> received.add(message.value()));
                assertThat(received).anyMatch(value -> value.contains(row.getEventId()));
            });
        }
    }

    @Test
    void liveBoardGetsTheLapWithinThreeSeconds() throws Exception {
        // the live listener only reads new events: it must own its partitions before the lap
        await().atMost(Duration.ofSeconds(30)).until(() -> listeners.getListenerContainers().stream()
                .allMatch(container -> container.getAssignedPartitions() != null
                        && !container.getAssignedPartitions().isEmpty()));
        RaceSession session = data.practiceSession(5);
        String admin = data.token(data.admin());
        Long bookingId = book(data.driver(), session, 71);
        approve(admin, bookingId);
        assertThat(status(post("/api/race-control/" + bookingId + "/start", admin, null))).isEqualTo(200);

        List<String> boards = new CopyOnWriteArrayList<>();
        HttpRequest watch = HttpRequest.newBuilder(URI.create(rest.getRootUri() + "/api/live/session/"
                + session.getSessionId())).header("Accept", "text/event-stream").build();
        Thread reader = new Thread(() -> HttpClient.newHttpClient()
                .sendAsync(watch, HttpResponse.BodyHandlers.ofLines())
                .join()
                .body()
                .filter(line -> line.startsWith("data:"))
                .forEach(boards::add));
        reader.setDaemon(true);
        reader.start();
        // the board as it is now, right after connecting
        await().atMost(Duration.ofSeconds(5)).until(() -> !boards.isEmpty());

        assertThat(status(post("/api/race-control/" + bookingId + "/lap", admin, null))).isEqualTo(200);

        await().atMost(Duration.ofSeconds(3))
                .until(() -> boards.stream().anyMatch(board -> board.contains("\"lapCount\":1")));
        // this instance pushed it at once, kafka brings the same board a moment later (what the other instance uses)
        await().atMost(Duration.ofSeconds(5))
                .until(() -> boards.stream().filter(board -> board.contains("\"lapCount\":1")).count() >= 2);
    }
}
