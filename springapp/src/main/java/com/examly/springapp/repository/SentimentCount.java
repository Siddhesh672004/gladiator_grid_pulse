package com.examly.springapp.repository;

import com.examly.springapp.model.Sentiment;

// feedback per ai label, null = a row from before the ai
public interface SentimentCount {

    Sentiment getSentiment();

    long getAmount();
}
