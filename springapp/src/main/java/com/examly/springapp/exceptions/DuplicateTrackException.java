package com.examly.springapp.exceptions;

public final class DuplicateTrackException extends GridPulseException {

    public DuplicateTrackException(String message) {
        super(message);
    }

    public DuplicateTrackException(String message, Throwable cause) {
        super(message, cause);
    }
}
