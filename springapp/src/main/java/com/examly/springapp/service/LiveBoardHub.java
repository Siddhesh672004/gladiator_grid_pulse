package com.examly.springapp.service;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.examly.springapp.dto.response.LiveStandingResponse;
import com.examly.springapp.exceptions.GatewayException;
import com.gridpulse.common.event.StandingsChanged;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// the open live boards of THIS instance (server-sent events): sessionId -> its viewers
// a change sends the whole board (the rows of GET /api/race-control/session/{id}), read once for all viewers
@Slf4j
@Component
@RequiredArgsConstructor
public class LiveBoardHub {

    private static final long TIMEOUT_MS = Duration.ofMinutes(30).toMillis();
    private static final int MAX_VIEWERS = 200;

    private final RaceControlService raceControlService;
    private final Map<Long, List<SseEmitter>> viewers = new ConcurrentHashMap<>();

    // the board right away, then every change. unknown session -> 404, a full board -> 503
    public SseEmitter subscribe(Long sessionId) {
        List<LiveStandingResponse> board = raceControlService.getLiveStandings(sessionId);
        List<SseEmitter> watching = viewers.computeIfAbsent(sessionId, id -> new CopyOnWriteArrayList<>());
        Optional.of(watching)
                .filter(list -> list.size() < MAX_VIEWERS)
                .orElseThrow(() -> new GatewayException("This live board has too many viewers right now. Please try "
                        + "again in a minute."));
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        watching.add(emitter);
        emitter.onCompletion(() -> watching.remove(emitter));
        emitter.onTimeout(() -> watching.remove(emitter));
        emitter.onError(error -> watching.remove(emitter));
        send(watching, emitter, board);
        return emitter;
    }

    // the change was made on this instance: its viewers see it at once (kafka brings it to the other instances)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onLocalChange(StandingsChanged event) {
        push(event.sessionId());
    }

    // never lets a broken board break the caller (an admin's lap, the kafka listener)
    public void push(Long sessionId) {
        List<SseEmitter> watching = viewers.getOrDefault(sessionId, List.of());
        if (watching.isEmpty()) {
            return;
        }
        try {
            List<LiveStandingResponse> board = raceControlService.getLiveStandings(sessionId);
            watching.forEach(emitter -> send(watching, emitter, board));
        } catch (RuntimeException e) {
            log.warn("Live board of session {} could not be sent: {}", sessionId, e.getMessage());
        }
    }

    // proxies close a connection that stays quiet, a comment line every 25 s keeps it open
    @Scheduled(fixedRate = 25_000)
    public void heartbeat() {
        viewers.values().forEach(watching -> watching.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event().comment("ping"));
            } catch (IOException | IllegalStateException e) {
                watching.remove(emitter);
            }
        }));
    }

    // shutting down: the open boards are closed at once (the browsers connect again, to another instance),
    // else the graceful shutdown waits for every open stream
    @EventListener(ContextClosedEvent.class)
    public void closeAll() {
        viewers.values().forEach(watching -> watching.forEach(SseEmitter::complete));
        viewers.clear();
    }

    // a viewer that went away is dropped
    private void send(List<SseEmitter> watching, SseEmitter emitter, List<LiveStandingResponse> board) {
        try {
            emitter.send(SseEmitter.event().name("standings").data(board, MediaType.APPLICATION_JSON));
        } catch (IOException | IllegalStateException e) {
            watching.remove(emitter);
        }
    }
}
