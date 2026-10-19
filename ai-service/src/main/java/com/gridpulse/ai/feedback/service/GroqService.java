package com.gridpulse.ai.feedback.service;

// the ai behind the feedback labels, suggestions and summary (the chats use gemini)
// instructions = ours, text = written by a user, always two separate messages. never throws
public interface GroqService {

    boolean isEnabled();

    // "" when the ai cannot answer, so feedback keeps working without it
    String ask(String instructions, String text);

    // the answer read into a record (structured output), null when there is none
    <T> T askFor(String instructions, String text, Class<T> type);
}
