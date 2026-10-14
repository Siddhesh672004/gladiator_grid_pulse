package com.examly.springapp.service;

import java.util.List;

import com.examly.springapp.dto.request.ChatRequest;
import com.examly.springapp.dto.response.ChatMessageResponse;
import com.examly.springapp.dto.response.ChatResponse;

// chatbot conversation, the work is in ChatServiceImpl
public interface ChatService {

    ChatResponse chat(ChatRequest request);

    List<ChatMessageResponse> getHistory(String sessionId);

    void clearMemory(String sessionId);
}
