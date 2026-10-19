package com.gridpulse.core.feedback.data;

// feedback per ai label, null = a row from before the ai
public interface SentimentCount {

    Sentiment getSentiment();

    long getAmount();
}
