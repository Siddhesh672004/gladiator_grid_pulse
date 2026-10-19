package com.gridpulse.ai.platform.exception;

public final class ResourceNotFoundException extends GridPulseException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
