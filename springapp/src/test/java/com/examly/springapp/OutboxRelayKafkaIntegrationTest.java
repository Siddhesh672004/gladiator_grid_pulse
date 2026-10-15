package com.examly.springapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.kafka.KafkaContainer;

import com.examly.springapp.model.RaceSession;
import com.examly.springapp.support.IntegrationTest;
import com.gridpulse.common.event.Topics;
import com.gridpulse.common.outbox.OutboxEvent;
import com.gridpulse.common.outbox.OutboxRepository;

// the relay with a real kafka (its own container): the approval's outbox row goes out and gets its sent_at
class OutboxRelayKafkaIntegrationTest extends IntegrationTest {

    @ServiceConnection
    static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka:4.0.0");

    static {
        KAFKA.start();
    }

    // the other tests keep the relay off
    @DynamicPropertySource
    static void relayOn(DynamicPropertyRegistry registry) {
        registry.add("gridpulse.outbox.relay.enabled", () -> "true");
    }

    @Autowired
    private OutboxRepository outboxRepository;

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
}
