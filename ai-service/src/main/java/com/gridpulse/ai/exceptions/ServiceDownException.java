package com.gridpulse.ai.exceptions;

// the core or payment-service answered 5xx: counted by its circuit breaker, the user hears "unavailable"
public final class ServiceDownException extends GridPulseException {

    public ServiceDownException(String message) {
        super(message);
    }
}
