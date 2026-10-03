package com.examly.springapp.exceptions;

public final class EmailSendException extends GridPulseException {

    public EmailSendException(String message) {
        super(message);
    }

    public EmailSendException(String message, Throwable cause) {
        super(message, cause);
    }
}
