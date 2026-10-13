package com.examly.springapp.dto.response;

import com.examly.springapp.dto.request.RaceBookingRequest;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// what the chatbot sends back for one message
// source -> "gemini", "faq", "fallback" or "booking" (the reply came from the spring ai booking tool)
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

    // only set when the booking tool started a payment: the chat window opens Razorpay with the order
    // and sends the booking back to /api/payments/confirm
    private PaymentOrderResponse paymentOrder;
    private RaceBookingRequest booking;

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
