package com.examly.springapp.exceptions;

public final class RegistrationException extends GridPulseException {

    public RegistrationException(String message) {
        super(message);
    }

    public RegistrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
