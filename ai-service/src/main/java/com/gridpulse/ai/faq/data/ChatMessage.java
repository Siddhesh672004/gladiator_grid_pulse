package com.gridpulse.ai.faq.data;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "chat_messages", indexes = @Index(name = "idx_chat_session", columnList = "sessionId"))
@Getter
@Setter
@NoArgsConstructor
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String sessionId;

    @Column(length = 2000)
    private String userMessage;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String botReply;

    // null when no faq met the threshold
    private Long matchedFaqId;

    private Double similarityScore;

    private LocalDateTime createdAt;

    public ChatMessage(String sessionId, String userMessage, String botReply, Long matchedFaqId, Double similarityScore) {
        this(sessionId, userMessage, botReply, matchedFaqId, similarityScore, LocalDateTime.now());
    }

    public ChatMessage(String sessionId, String userMessage, String botReply, Long matchedFaqId, Double similarityScore,
            LocalDateTime createdAt) {
        this.sessionId = sessionId;
        this.userMessage = userMessage;
        this.botReply = botReply;
        this.matchedFaqId = matchedFaqId;
        this.similarityScore = similarityScore;
        this.createdAt = createdAt;
    }
}
