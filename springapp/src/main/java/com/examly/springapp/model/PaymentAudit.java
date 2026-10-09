package com.examly.springapp.model;

import java.time.Instant;
import java.util.Optional;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// audit trail of a payment, rows are only added, each in its own transaction (REQUIRES_NEW)
// razorpayOrderId is plain text, no foreign key: the caller holds a lock on the payment row, the insert would wait for itself
@Entity
@Getter
@Setter
@NoArgsConstructor
public class PaymentAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long auditId;

    @Column(length = 40)
    private String razorpayOrderId;

    @Column(length = 30)
    private String event;

    @Column(length = 500)
    private String detail;

    @JdbcTypeCode(SqlTypes.TIMESTAMP_UTC)
    private Instant createdAt;

    public PaymentAudit(String razorpayOrderId, String event, String detail) {
        this.razorpayOrderId = razorpayOrderId;
        this.event = event;
        // the column holds 500 characters, a longer text is cut
        this.detail = Optional.ofNullable(detail)
                .filter(text -> text.length() > 500)
                .map(text -> text.substring(0, 500))
                .orElse(detail);
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
