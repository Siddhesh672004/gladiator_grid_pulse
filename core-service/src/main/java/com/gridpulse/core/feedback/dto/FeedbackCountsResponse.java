package com.gridpulse.core.feedback.dto;

import java.util.Map;

import com.gridpulse.core.feedback.data.Sentiment;

// the counters above the admin feedback table, over ALL feedback (not only the page)
public record FeedbackCountsResponse(long total, Map<Sentiment, Long> sentiments, long urgent) {
}
