package com.gridpulse.ai.dto;

import com.gridpulse.ai.model.Sentiment;

// what groq answers for one feedback (structured output) and what /internal/ai/label sends back
public record FeedbackLabel(Sentiment sentiment, boolean urgent) {

    public static FeedbackLabel notAnalysed() {
        return new FeedbackLabel(Sentiment.NOT_ANALYSED, false);
    }
}
