package com.gridpulse.core.platform.exception;

public final class DuplicateCarNumberException extends GridPulseException {

    public DuplicateCarNumberException(String message) {
        super(message);
    }

    public DuplicateCarNumberException(String message, Throwable cause) {
        super(message, cause);
    }
}
