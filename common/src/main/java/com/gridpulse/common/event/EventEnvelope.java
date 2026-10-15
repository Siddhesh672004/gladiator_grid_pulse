package com.gridpulse.common.event;

import java.time.Instant;

import com.fasterxml.jackson.databind.JsonNode;

// every kafka message: who / what / when around the payload. type = the payload record's simple name (BookingDecided)
public record EventEnvelope(String eventId, String type, Instant occurredAt, String source, JsonNode data) {
}
