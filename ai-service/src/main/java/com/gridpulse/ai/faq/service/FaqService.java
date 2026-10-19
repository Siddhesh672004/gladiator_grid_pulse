package com.gridpulse.ai.faq.service;

import java.util.List;

import com.gridpulse.ai.faq.dto.FaqMatch;
import com.gridpulse.ai.faq.dto.FaqResponse;

// the chatbot knowledge base
public interface FaqService {

    // faqs.json -> faqs table, runs once on startup
    void seedFaqs();

    FaqMatch findBestMatch(String query, float[] queryEmbedding);

    List<FaqResponse> getAllFaqs();

    boolean hasEmbeddings();
}
