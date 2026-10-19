package com.gridpulse.core.exceptions;

// clashes with existing data -> 409 (overlapping session, session full, second feedback)
public final class ConflictException extends GridPulseException {

    public ConflictException(String message) {
        super(message);
    }

    public ConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
