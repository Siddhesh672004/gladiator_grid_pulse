package com.gridpulse.ai.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gridpulse.ai.entity.ChatMessage;

public interface ChatMessageRepo extends JpaRepository<ChatMessage, Long> {
}
