package com.gridpulse.common.outbox;

import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// one event waiting for kafka (sentAt null) or already sent. payload = the whole envelope as json
@Entity
@Table(name = "outbox_event")
@Getter
@Setter
@NoArgsConstructor
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, length = 36)
    private String eventId;

    @Column(nullable = false, length = 100)
    private String topic;

    @Column(name = "msg_key", length = 200)
    private String msgKey;

    @Column(nullable = false, length = 100)
    private String type;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String payload;

    @JdbcTypeCode(SqlTypes.TIMESTAMP_UTC)
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @JdbcTypeCode(SqlTypes.TIMESTAMP_UTC)
    @Column(name = "sent_at")
    private Instant sentAt;

    public OutboxEvent(String eventId, String topic, String msgKey, String type, String payload, Instant createdAt) {
        this.eventId = eventId;
        this.topic = topic;
        this.msgKey = msgKey;
        this.type = type;
        this.payload = payload;
        this.createdAt = createdAt;
    }
}
