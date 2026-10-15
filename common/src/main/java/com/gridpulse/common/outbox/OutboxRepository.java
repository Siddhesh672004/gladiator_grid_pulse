package com.gridpulse.common.outbox;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {

    // oldest first; a second relay (another instance) skips the rows this one holds instead of waiting
    @Query(value = "SELECT * FROM outbox_event WHERE sent_at IS NULL ORDER BY id LIMIT 100 FOR UPDATE SKIP LOCKED",
            nativeQuery = true)
    List<OutboxEvent> lockUnsent();
}
