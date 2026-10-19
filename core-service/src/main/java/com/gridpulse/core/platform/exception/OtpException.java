package com.gridpulse.core.platform.exception;

// wrong, expired or used up code (or asked too often) -> 400
public final class OtpException extends GridPulseException {

    public OtpException(String message) {
        super(message);
    }

    public OtpException(String message, Throwable cause) {
        super(message, cause);
    }
}
