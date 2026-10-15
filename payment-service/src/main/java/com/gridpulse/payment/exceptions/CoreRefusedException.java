package com.gridpulse.payment.exceptions;

import java.util.Map;

import lombok.Getter;

// the core answered 4xx (car number taken, session full ...): its status, message and field errors go to the driver
@Getter
public final class CoreRefusedException extends GridPulseException {

    private final int status;
    private final transient Map<String, String> fieldErrors;

    public CoreRefusedException(int status, String message, Map<String, String> fieldErrors) {
        super(message);
        this.status = status;
        this.fieldErrors = fieldErrors;
    }
}
