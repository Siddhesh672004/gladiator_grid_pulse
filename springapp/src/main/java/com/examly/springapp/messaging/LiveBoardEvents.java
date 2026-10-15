package com.examly.springapp.messaging;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.gridpulse.common.event.StandingsChanged;
import com.gridpulse.common.event.Topics;
import com.gridpulse.common.outbox.EventPublisher;

import lombok.RequiredArgsConstructor;

// a board changed: kafka tells every core instance (outbox, inside the caller's transaction), and this instance's
// own viewers get it right after the commit (LiveBoardHub), so one instance without kafka still has live boards
@Component
@RequiredArgsConstructor
public class LiveBoardEvents {

    private final EventPublisher eventPublisher;
    private final ApplicationEventPublisher applicationEventPublisher;

    public void changed(Long sessionId, String reason) {
        StandingsChanged event = new StandingsChanged(sessionId, reason);
        eventPublisher.publish(Topics.RACE, String.valueOf(sessionId), event);
        applicationEventPublisher.publishEvent(event);
    }
}
