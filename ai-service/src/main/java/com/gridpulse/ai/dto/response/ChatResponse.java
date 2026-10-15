package com.gridpulse.ai.dto.response;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// what the chatbot sends back for one message
// source -> "gemini", "faq" or "fallback". booking by chat is the logged-in Copilot now (/api/ai/copilot)
@Getter
@Setter
@NoArgsConstructor
public class ChatResponse {

    private String reply;
    private boolean matched;
    private String matchedQuestion;
    private String category;
    private double confidence;
    private String source;
    private String sessionId;
    private String resolvedQuestion;

    // always empty since the faq chat stopped booking; kept so the json keeps its shape
    private Object paymentOrder;
    private Object booking;

    // no faq matched
    public ChatResponse(String reply, String source, String sessionId, String resolvedQuestion) {
        this(reply, false, null, null, 0.0, source, sessionId, resolvedQuestion);
    }

    public ChatResponse(String reply, boolean matched, String matchedQuestion, String category, double confidence,
            String source, String sessionId, String resolvedQuestion) {
        this.reply = reply;
        this.matched = matched;
        this.matchedQuestion = matchedQuestion;
        this.category = category;
        this.confidence = confidence;
        this.source = source;
        this.sessionId = sessionId;
        this.resolvedQuestion = resolvedQuestion;
    }
}
