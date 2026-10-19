package com.gridpulse.ai.mapper;

import org.springframework.stereotype.Component;

import com.gridpulse.ai.dto.response.FaqResponse;
import com.gridpulse.ai.entity.FaqEntity;

@Component
public class ChatMapper {

    public FaqResponse toResponse(FaqEntity faq) {
        return new FaqResponse(faq.getId(), faq.getCategory(), faq.getQuestion(), faq.getAnswer());
    }
}
