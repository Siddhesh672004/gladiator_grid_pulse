package com.gridpulse.ai.mapper;

import org.springframework.stereotype.Component;

import com.gridpulse.ai.dto.response.ChatMessageResponse;
import com.gridpulse.ai.dto.response.FaqResponse;
import com.gridpulse.ai.entity.ChatMessage;
import com.gridpulse.ai.entity.FaqEntity;

@Component
public class ChatMapper {

    public ChatMessageResponse toResponse(ChatMessage message) {
        return new ChatMessageResponse(message.getId(), message.getSessionId(), message.getUserMessage(),
                message.getBotReply(), message.getMatchedFaqId(), message.getSimilarityScore(),
                message.getCreatedAt());
    }

    public FaqResponse toResponse(FaqEntity faq) {
        return new FaqResponse(faq.getId(), faq.getCategory(), faq.getQuestion(), faq.getAnswer());
    }
}
