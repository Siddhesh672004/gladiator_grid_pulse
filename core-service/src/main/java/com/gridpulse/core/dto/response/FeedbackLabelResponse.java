package com.gridpulse.core.dto.response;

// ai-service's /internal/ai/label answer: POSITIVE / NEGATIVE / NEUTRAL / NOT_ANALYSED + urgent
public record FeedbackLabelResponse(String sentiment, boolean urgent) {
}
