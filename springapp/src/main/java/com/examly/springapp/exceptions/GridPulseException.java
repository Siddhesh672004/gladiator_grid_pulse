package com.examly.springapp.exceptions;

// parent of every business exception; GlobalExceptionHandler gives each child its http status
public abstract sealed class GridPulseException extends RuntimeException
        permits BookingException, ConflictException, DuplicateCarNumberException, DuplicateTrackException, EmailSendException,
        GatewayException, GeminiException, OtpException, RegistrationException, ResourceNotFoundException,
        TrackDeletionException {

    protected GridPulseException(String message) {
        super(message);
    }

    protected GridPulseException(String message, Throwable cause) {
        super(message, cause);
    }
}
