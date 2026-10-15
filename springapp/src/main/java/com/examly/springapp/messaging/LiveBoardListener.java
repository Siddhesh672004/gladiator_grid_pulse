package com.examly.springapp.messaging;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.examly.springapp.service.LiveBoardHub;
import com.gridpulse.common.event.EventEnvelope;
import com.gridpulse.common.event.EventReader;
import com.gridpulse.common.event.StandingsChanged;
import com.gridpulse.common.event.Topics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// every core instance has its own consumer group (core-live-<random>), so each one hears every board change and
// pushes it to its own viewers. only new changes matter (latest), a missed one is not tried again
@Slf4j
@Component
@RequiredArgsConstructor
public class LiveBoardListener {

    private final EventReader eventReader;
    private final LiveBoardHub liveBoardHub;

    @KafkaListener(topics = Topics.RACE, groupId = "core-live-${random.uuid}", properties = "auto.offset.reset=latest")
    public void onRaceEvent(String message) {
        EventEnvelope event = eventReader.read(message);
        if ("StandingsChanged".equals(event.type())) {
            liveBoardHub.push(eventReader.payload(event, StandingsChanged.class).sessionId());
        } else {
            log.debug("Event type {} is not for the live boards, skipped", event.type());
        }
    }
}
