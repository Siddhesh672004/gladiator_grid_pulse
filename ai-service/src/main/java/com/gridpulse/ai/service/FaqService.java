package com.gridpulse.ai.service;

import java.util.List;

import com.gridpulse.ai.dto.FaqMatch;
import com.gridpulse.ai.dto.response.FaqResponse;

// the chatbot knowledge base
public interface FaqService {

    // faqs.json -> faqs table, runs once on startup
    void seedFaqs();

    FaqMatch findBestMatch(String query, float[] queryEmbedding);

    List<FaqResponse> getAllFaqs();

    boolean hasEmbeddings();
}
