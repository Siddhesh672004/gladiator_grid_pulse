package com.gridpulse.ai.faq.dto;

import com.gridpulse.ai.faq.data.FaqEntity;

// result of FaqService.findBestMatch, faq / score are filled even when matched = false
// method = "semantic" (embeddings), "lexical" (word overlap) or "none"
public record FaqMatch(FaqEntity faq, double score, String method, boolean matched) {

    public FaqMatch(String method) {
        this(null, 0.0, method, false);
    }
}
