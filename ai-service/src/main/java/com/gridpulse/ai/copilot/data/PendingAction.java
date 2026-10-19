package com.gridpulse.ai.copilot.data;

import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// a change the Copilot prepared: nothing happens until its owner presses Confirm (once, within 5 minutes)
// token = 32 random bytes, base64url: the only way to name it
@Entity
@Table(name = "pending_action")
@Getter
@Setter
@NoArgsConstructor
public class PendingAction {

    @Id
    @Column(length = 64)
    private String token;

    private int userId;

    @Column(length = 20)
    private String role;

    @Column(length = 60)
    private String toolName;

    @Column(columnDefinition = "TEXT")
    private String argsJson;

    @Column(length = 500)
    private String summary;

    @JdbcTypeCode(SqlTypes.TIMESTAMP_UTC)
    private Instant createdAt;

    @JdbcTypeCode(SqlTypes.TIMESTAMP_UTC)
    private Instant expiresAt;

    // confirmed, cancelled or found expired: never again
    @JdbcTypeCode(SqlTypes.TIMESTAMP_UTC)
    private Instant usedAt;

    public PendingAction(String token, int userId, String role, String toolName, String argsJson, String summary,
            Instant createdAt, Instant expiresAt) {
        this.token = token;
        this.userId = userId;
        this.role = role;
        this.toolName = toolName;
        this.argsJson = argsJson;
        this.summary = summary;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }
}
