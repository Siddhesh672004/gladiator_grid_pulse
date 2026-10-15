package com.gridpulse.common.outbox;

import java.time.Instant;
import java.util.UUID;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.gridpulse.common.event.EventEnvelope;
import com.gridpulse.common.event.EventJson;

import lombok.RequiredArgsConstructor;

// the only way to publish: the event row is saved in the caller's transaction, together with the business change
// or not at all. OutboxRelay sends it to kafka a moment later. no transaction = a bug -> exception
@RequiredArgsConstructor
public class EventPublisher {

    private final OutboxRepository outboxRepository;
    private final String source;

    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(String topic, String key, Object payload) {
        EventEnvelope envelope = new EventEnvelope(UUID.randomUUID().toString(), payload.getClass().getSimpleName(),
                Instant.now(), source, EventJson.MAPPER.valueToTree(payload));
        outboxRepository.save(new OutboxEvent(envelope.eventId(), topic, key, envelope.type(), toJson(envelope),
                envelope.occurredAt()));
    }

    private String toJson(EventEnvelope envelope) {
        try {
            return EventJson.MAPPER.writeValueAsString(envelope);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Event " + envelope.type() + " could not be written as json", e);
        }
    }
}
