package com.examly.springapp.exceptions;

public final class DuplicateCarNumberException extends GridPulseException {

    public DuplicateCarNumberException(String message) {
        super(message);
    }

    public DuplicateCarNumberException(String message, Throwable cause) {
        super(message, cause);
    }
}
