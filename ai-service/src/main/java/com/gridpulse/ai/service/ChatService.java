package com.gridpulse.ai.service;

import java.util.List;

import com.gridpulse.ai.dto.request.ChatRequest;
import com.gridpulse.ai.dto.response.ChatMessageResponse;
import com.gridpulse.ai.dto.response.ChatResponse;

// chatbot conversation, the work is in ChatServiceImpl
public interface ChatService {

    ChatResponse chat(ChatRequest request);

    List<ChatMessageResponse> getHistory(String sessionId);

    void clearMemory(String sessionId);
}
