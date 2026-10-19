package com.gridpulse.ai.copilot.data;

import java.time.Instant;
import java.util.Optional;

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

// what became of every prepared action: OK, ERROR, CANCELLED or EXPIRED. rows are only added
@Entity
@Table(name = "ai_action_log")
@Getter
@NoArgsConstructor
public class ActionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int userId;

    @Column(length = 20)
    private String role;

    @Column(length = 60)
    private String toolName;

    @Column(columnDefinition = "TEXT")
    private String argsJson;

    @Column(length = 20)
    private String outcome;

    @Column(length = 500)
    private String message;

    @JdbcTypeCode(SqlTypes.TIMESTAMP_UTC)
    private Instant createdAt;

    public ActionLog(PendingAction action, String outcome, String message) {
        this.userId = action.getUserId();
        this.role = action.getRole();
        this.toolName = action.getToolName();
        this.argsJson = action.getArgsJson();
        this.outcome = outcome;
        // the column holds 500 characters, a longer message is cut
        this.message = Optional.ofNullable(message)
                .filter(text -> text.length() > 500)
                .map(text -> text.substring(0, 500))
                .orElse(message);
        this.createdAt = Instant.now();
    }
}
