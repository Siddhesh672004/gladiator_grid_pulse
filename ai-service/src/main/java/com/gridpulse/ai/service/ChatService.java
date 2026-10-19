package com.gridpulse.ai.service;

import com.gridpulse.ai.dto.request.ChatRequest;
import com.gridpulse.ai.dto.response.ChatResponse;

// chatbot conversation, the work is in ChatServiceImpl
public interface ChatService {

    ChatResponse chat(ChatRequest request);

    void clearMemory(String sessionId);
}
