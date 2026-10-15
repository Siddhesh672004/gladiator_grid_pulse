package com.gridpulse.common.event;

// a live board changed (start, lap, retire, reset, finish ...): every core instance pushes the new board to its viewers
public record StandingsChanged(Long sessionId, String reason) {
}
