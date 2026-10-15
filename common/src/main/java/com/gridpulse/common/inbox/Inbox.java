package com.gridpulse.common.inbox;

import java.time.Instant;

import lombok.RequiredArgsConstructor;

// at-least-once kafka -> handle each event once per consumer: isNew before the work, markProcessed after it
@RequiredArgsConstructor
public class Inbox {

    private final ProcessedEventRepository processedEventRepository;

    public boolean isNew(String eventId, String consumer) {
        return !processedEventRepository.existsById(new ProcessedEventKey(eventId, consumer));
    }

    public void markProcessed(String eventId, String consumer) {
        processedEventRepository.save(new ProcessedEvent(eventId, consumer, Instant.now()));
    }
}
