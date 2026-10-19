package com.gridpulse.ai.faq.service;

import com.gridpulse.ai.faq.dto.ChatRequest;
import com.gridpulse.ai.faq.dto.ChatResponse;

// chatbot conversation, the work is in ChatServiceImpl
public interface ChatService {

    ChatResponse chat(ChatRequest request);

    void clearMemory(String sessionId);
}
