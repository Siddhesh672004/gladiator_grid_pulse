package com.examly.springapp.service;

// the ai behind the feedback labels (the chatbot uses gemini instead)
public interface GroqService {

    boolean isEnabled();

    // instructions = ours, text = written by a user. never throws: "" when the ai cannot answer,
    // so feedback keeps working without it
    String ask(String instructions, String text);
}
