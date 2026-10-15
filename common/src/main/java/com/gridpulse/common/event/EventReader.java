package com.gridpulse.common.event;

import com.fasterxml.jackson.core.JsonProcessingException;

// consumer side: kafka message text -> envelope -> payload record
public class EventReader {

    // not json / no envelope -> IllegalArgumentException
    public EventEnvelope read(String json) {
        try {
            return EventJson.MAPPER.readValue(json, EventEnvelope.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Not an event envelope", e);
        }
    }

    public <T> T payload(EventEnvelope envelope, Class<T> type) {
        try {
            return EventJson.MAPPER.treeToValue(envelope.data(), type);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Event " + envelope.eventId() + " is not a " + type.getSimpleName(), e);
        }
    }
}
