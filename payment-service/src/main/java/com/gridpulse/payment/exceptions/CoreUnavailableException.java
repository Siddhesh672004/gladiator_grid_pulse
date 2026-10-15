package com.gridpulse.payment.exceptions;

// the core did not answer in time, answered 5xx, or its breaker is open -> 503 (confirm turns it into a 202)
public final class CoreUnavailableException extends GridPulseException {

    public CoreUnavailableException(String message) {
        super(message);
    }

    public CoreUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
