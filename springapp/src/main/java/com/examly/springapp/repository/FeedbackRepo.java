package com.examly.springapp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.examly.springapp.model.Feedback;

public interface FeedbackRepo extends JpaRepository<Feedback, Long> {

    List<Feedback> findByUserUserId(int userId);

    List<Feedback> findBySessionSessionId(Long sessionId);

    boolean existsBySessionSessionIdAndUserUserId(Long sessionId, int userId);

    // what the ai summary reads
    List<Feedback> findTop50ByOrderByFeedbackIdDesc();

    long countByUrgentTrue();
}
