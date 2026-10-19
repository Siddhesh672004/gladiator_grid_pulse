package com.gridpulse.ai.faq.service;

import org.springframework.stereotype.Component;

import com.gridpulse.ai.faq.data.FaqEntity;
import com.gridpulse.ai.faq.dto.FaqResponse;

@Component
public class ChatMapper {

    public FaqResponse toResponse(FaqEntity faq) {
        return new FaqResponse(faq.getId(), faq.getCategory(), faq.getQuestion(), faq.getAnswer());
    }
}
