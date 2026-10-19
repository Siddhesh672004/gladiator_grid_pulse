package com.gridpulse.ai.platform.exception;

// the core or payment-service answered 5xx: counted by its circuit breaker, the user hears "unavailable"
public final class ServiceDownException extends GridPulseException {

    public ServiceDownException(String message) {
        super(message);
    }
}
