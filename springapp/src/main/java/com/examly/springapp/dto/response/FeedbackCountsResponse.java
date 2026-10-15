package com.examly.springapp.dto.response;

import java.util.Map;

import com.examly.springapp.model.Sentiment;

// the counters above the admin feedback table, over ALL feedback (not only the page)
public record FeedbackCountsResponse(long total, Map<Sentiment, Long> sentiments, long urgent) {
}
