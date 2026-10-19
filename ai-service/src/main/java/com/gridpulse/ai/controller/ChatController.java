package com.gridpulse.ai.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.gridpulse.ai.dto.request.ChatRequest;
import com.gridpulse.ai.dto.response.ChatResponse;
import com.gridpulse.ai.dto.response.FaqResponse;
import com.gridpulse.ai.service.ChatService;
import com.gridpulse.ai.service.FaqService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final FaqService faqService;

    @PostMapping("/api/chat")
    public ResponseEntity<ChatResponse> sendMessage(@Valid @RequestBody ChatRequest request) {
        return ResponseEntity.ok(chatService.chat(request));
    }

    @DeleteMapping("/api/chat/memory/{sessionId}")
    public ResponseEntity<Void> clearMemory(@PathVariable String sessionId) {
        chatService.clearMemory(sessionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/faqs")
    public ResponseEntity<List<FaqResponse>> getAllFaqs() {
        return ResponseEntity.ok(faqService.getAllFaqs());
    }
}
