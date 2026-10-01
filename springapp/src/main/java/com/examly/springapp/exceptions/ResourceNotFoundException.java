package com.examly.springapp.exceptions;

public final class ResourceNotFoundException extends GridPulseException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
