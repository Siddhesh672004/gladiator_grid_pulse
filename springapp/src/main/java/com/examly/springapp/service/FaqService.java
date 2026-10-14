package com.examly.springapp.service;

import java.util.List;

import com.examly.springapp.dto.FaqMatch;
import com.examly.springapp.dto.response.FaqResponse;

// the chatbot knowledge base
public interface FaqService {

    // faqs.json -> faqs table, runs once on startup
    void seedFaqs();

    FaqMatch findBestMatch(String query, float[] queryEmbedding);

    List<FaqResponse> getAllFaqs();

    boolean hasEmbeddings();
}
