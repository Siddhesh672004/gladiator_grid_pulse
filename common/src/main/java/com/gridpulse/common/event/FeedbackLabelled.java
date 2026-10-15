package com.gridpulse.common.event;

// ai-service: the label of a feedback; sentiment = POSITIVE / NEGATIVE / NEUTRAL / NOT_ANALYSED
public record FeedbackLabelled(Long feedbackId, String sentiment, boolean urgent) {
}
