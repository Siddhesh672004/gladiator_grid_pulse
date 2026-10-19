package com.gridpulse.core.platform.exception;

// bad booking data or a race control action that is not allowed -> 400
public final class BookingException extends GridPulseException {

    public BookingException(String message) {
        super(message);
    }

    public BookingException(String message, Throwable cause) {
        super(message, cause);
    }
}
