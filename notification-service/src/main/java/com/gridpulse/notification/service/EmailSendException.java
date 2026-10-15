package com.gridpulse.notification.service;

// gmail did not take the email; the kafka listener lets it fail, so the retry topics try again later
public class EmailSendException extends RuntimeException {

    public EmailSendException(String message, Throwable cause) {
        super(message, cause);
    }
}
