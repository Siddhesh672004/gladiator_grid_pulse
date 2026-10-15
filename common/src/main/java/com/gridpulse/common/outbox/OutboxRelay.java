package com.gridpulse.common.outbox;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// every 500 ms: unsent outbox rows -> kafka, in id order. the first failure stops the batch (the rows before it
// stay sent, the rest is tried again next time), so the order per key never changes
// off with gridpulse.outbox.relay.enabled=false (tests, a service that does not publish)
@Slf4j
@RequiredArgsConstructor
public class OutboxRelay {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    // one warning per outage, not one every half second
    private boolean kafkaDown;

    @Scheduled(fixedDelay = 500)
    @Transactional
    public void relay() {
        List<OutboxEvent> waiting = outboxRepository.lockUnsent();
        for (OutboxEvent row : waiting) {
            if (!send(row)) {
                return;
            }
            row.setSentAt(Instant.now());
        }
        if (kafkaDown && !waiting.isEmpty()) {
            kafkaDown = false;
            log.info("Kafka reachable again, outbox is being sent");
        }
    }

    private boolean send(OutboxEvent row) {
        try {
            kafkaTemplate.send(row.getTopic(), row.getMsgKey(), row.getPayload()).get(5, TimeUnit.SECONDS);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            if (!kafkaDown) {
                kafkaDown = true;
                log.warn("Outbox event {} ({}) could not be sent to {}: {} - trying again every 500 ms",
                        row.getEventId(), row.getType(), row.getTopic(), e.getClass().getSimpleName());
            }
            return false;
        }
    }
}
