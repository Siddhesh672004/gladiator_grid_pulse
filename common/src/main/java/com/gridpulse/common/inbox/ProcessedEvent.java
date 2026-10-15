package com.gridpulse.common.inbox;

import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// an event this consumer already handled: kafka may deliver it again, the second time is skipped
@Entity
@Table(name = "processed_event")
@IdClass(ProcessedEventKey.class)
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedEvent {

    @Id
    @Column(name = "event_id", length = 36)
    private String eventId;

    @Id
    @Column(length = 100)
    private String consumer;

    @JdbcTypeCode(SqlTypes.TIMESTAMP_UTC)
    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;
}
