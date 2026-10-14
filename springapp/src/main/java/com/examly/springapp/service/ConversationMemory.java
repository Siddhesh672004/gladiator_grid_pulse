package com.examly.springapp.service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

// short-term chat memory, one small history per sessionId. the full transcript is in the chat_messages table
@Slf4j
@Component
public class ConversationMemory {

    public static final int MAX_TURNS = 8;

    // least recently used session is dropped first, so the public chat endpoint can't fill the memory
    private static final int MAX_SESSIONS = 1000;

    // resolvedQuestion = the follow-up rewritten as a standalone question
    public record Turn(String userMessage, String resolvedQuestion, String botReply) {
    }

    private final Map<String, Deque<Turn>> sessions = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Deque<Turn>> eldest) {
            return size() > MAX_SESSIONS;
        }
    };

    public List<Turn> getHistory(String sessionId) {
        synchronized (sessions) {
            // a copy the caller may change, no history yet = empty
            return Optional.ofNullable(sessions.get(sessionId))
                    .<List<Turn>>map(ArrayList::new)
                    .orElse(List.of());
        }
    }

    public void addTurn(String sessionId, Turn turn) {
        synchronized (sessions) {
            Deque<Turn> turns = sessions.computeIfAbsent(sessionId, id -> new ArrayDeque<>());
            turns.addLast(turn);
            // only the newest MAX_TURNS stay, the oldest go first
            IntStream.range(0, Math.max(0, turns.size() - MAX_TURNS))
                    .forEach(i -> turns.removeFirst());
            log.trace("session {} now remembers {} turn(s)", sessionId, turns.size());
        }
    }

    public void clear(String sessionId) {
        synchronized (sessions) {
            sessions.remove(sessionId);
        }
        log.info("Chat memory cleared for session {}", sessionId);
    }
}
