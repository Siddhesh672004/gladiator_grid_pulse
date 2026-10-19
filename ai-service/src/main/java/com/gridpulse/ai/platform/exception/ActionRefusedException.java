package com.gridpulse.ai.platform.exception;

import lombok.Getter;

// a Confirm / Cancel that is not possible: unknown token (404), already used (409), expired (410)
@Getter
public final class ActionRefusedException extends GridPulseException {

    private final int status;

    public ActionRefusedException(int status, String message) {
        super(message);
        this.status = status;
    }
}
