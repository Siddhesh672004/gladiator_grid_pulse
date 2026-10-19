package com.gridpulse.core.dto.response;

import java.util.Map;

import com.gridpulse.core.model.Sentiment;

// the counters above the admin feedback table, over ALL feedback (not only the page)
public record FeedbackCountsResponse(long total, Map<Sentiment, Long> sentiments, long urgent) {
}
