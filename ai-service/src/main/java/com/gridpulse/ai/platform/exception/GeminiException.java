package com.gridpulse.ai.platform.exception;

// gemini is down or answered with an error; ChatService catches it and falls back to the faq answer
public final class GeminiException extends GridPulseException {

    public GeminiException(String message) {
        super(message);
    }

    public GeminiException(String message, Throwable cause) {
        super(message, cause);
    }
}
