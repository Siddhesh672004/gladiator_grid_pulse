package com.gridpulse.ai.exceptions;

// parent of every business exception; GlobalExceptionHandler gives each child its http status
public abstract sealed class GridPulseException extends RuntimeException
        permits GeminiException, ResourceNotFoundException, ServiceDownException, ToolCallException {

    protected GridPulseException(String message) {
        super(message);
    }

    protected GridPulseException(String message, Throwable cause) {
        super(message, cause);
    }
}
