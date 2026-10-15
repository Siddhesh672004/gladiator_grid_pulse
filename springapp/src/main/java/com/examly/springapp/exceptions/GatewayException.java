package com.examly.springapp.exceptions;

// cloudinary or payment-service is not set up, not reachable or refused the request -> 503 (reason is in the log)
public final class GatewayException extends GridPulseException {

    public GatewayException(String message) {
        super(message);
    }

    public GatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}
