package com.examly.springapp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.examly.springapp.entity.ChatMessage;

public interface ChatMessageRepo extends JpaRepository<ChatMessage, Long> {

    // oldest first, the id breaks ties inside the same millisecond
    List<ChatMessage> findBySessionIdOrderByCreatedAtAscIdAsc(String sessionId);
}
