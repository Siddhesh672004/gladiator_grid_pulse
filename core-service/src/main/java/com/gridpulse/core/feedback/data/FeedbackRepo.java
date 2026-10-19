package com.gridpulse.core.feedback.data;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FeedbackRepo extends JpaRepository<Feedback, Long> {

    List<Feedback> findByUserUserId(int userId);

    List<Feedback> findBySessionSessionId(Long sessionId);

    boolean existsBySessionSessionIdAndUserUserId(Long sessionId, int userId);

    // what the ai summary reads
    List<Feedback> findTop50ByOrderByFeedbackIdDesc();

    long countByUrgentTrue();

    // admin list page. notAnalysed = also the old rows without a label. search: driver, track or text
    @EntityGraph(attributePaths = { "user", "session", "session.track", "session.competition" })
    @Query("select f from Feedback f left join f.session s left join s.track t"
            + " where (?1 is null or f.sentiment = ?1 or (?2 = true and f.sentiment is null))"
            + " and (?3 is null or f.rating = ?3) and (?4 = false or f.urgent = true)"
            + " and (lower(f.user.username) like ?5 or lower(coalesce(t.trackName, '')) like ?5"
            + " or lower(coalesce(f.feedbackText, '')) like ?5)")
    Page<Feedback> searchPage(Sentiment sentiment, boolean notAnalysed, Integer rating, boolean urgentOnly,
            String pattern, Pageable pageable);

    @Query("select f.sentiment as sentiment, count(f) as amount from Feedback f group by f.sentiment")
    List<SentimentCount> countPerSentiment();
}
