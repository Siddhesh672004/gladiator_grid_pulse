package com.gridpulse.core.repository;

import com.gridpulse.core.model.Sentiment;

// feedback per ai label, null = a row from before the ai
public interface SentimentCount {

    Sentiment getSentiment();

    long getAmount();
}
