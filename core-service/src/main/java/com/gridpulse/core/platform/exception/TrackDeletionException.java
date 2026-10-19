package com.gridpulse.core.platform.exception;

// track that already had a session cannot be deleted -> 409
public final class TrackDeletionException extends GridPulseException {

    public TrackDeletionException(String message) {
        super(message);
    }

    public TrackDeletionException(String message, Throwable cause) {
        super(message, cause);
    }
}
