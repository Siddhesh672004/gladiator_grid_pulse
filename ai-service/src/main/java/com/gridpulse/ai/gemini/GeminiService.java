package com.gridpulse.ai.gemini;

// the one place that talks to google gemini, GeminiServiceImpl has the actual code
public interface GeminiService {

    // both throw GeminiException on failure
    float[] embed(String text);

    String generate(String prompt);

    boolean isEnabled();
}
