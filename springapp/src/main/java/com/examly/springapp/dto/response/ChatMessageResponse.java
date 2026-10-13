package com.examly.springapp.dto.response;

import java.time.LocalDateTime;

public record ChatMessageResponse(
        Long id,
        String sessionId,
        String userMessage,
        String botReply,
        Long matchedFaqId,
        Double similarityScore,
        LocalDateTime createdAt) {
}
