package com.gridpulse.common.event;

// core: a driver saved feedback (NOT_ANALYSED), ai-service labels it a moment later
public record FeedbackSubmitted(Long feedbackId, String text) {
}
