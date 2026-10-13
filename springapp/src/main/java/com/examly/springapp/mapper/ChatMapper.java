package com.examly.springapp.mapper;

import org.springframework.stereotype.Component;

import com.examly.springapp.dto.response.ChatMessageResponse;
import com.examly.springapp.dto.response.FaqResponse;
import com.examly.springapp.entity.ChatMessage;
import com.examly.springapp.entity.FaqEntity;

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
