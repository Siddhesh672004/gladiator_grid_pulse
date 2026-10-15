package com.gridpulse.ai.exceptions;

import lombok.Getter;

// the core or payment-service answered 4xx: its own message (car number taken, not your booking ...)
// the Copilot passes the message to the model, which tells the user
@Getter
public final class ToolCallException extends GridPulseException {

    private final int status;

    public ToolCallException(int status, String message) {
        super(message);
        this.status = status;
    }
}
