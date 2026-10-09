package com.examly.springapp.model;

import java.time.Instant;
import java.util.Optional;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// made BEFORE the driver pays (status CREATED), the booking only exists once the payment is confirmed
// never deleted: the money history stays even when the booking is cancelled / deleted
@Entity
@Getter
@Setter
@NoArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentId;

    @ManyToOne
    @JoinColumn(name = "userId")
    private User user;

    @ManyToOne
    @JoinColumn(name = "sessionId")
    private RaceSession session;

    // in rupees
    private Double amount;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PaymentStatus status = PaymentStatus.CREATED;

    @Column(unique = true, length = 40)
    private String razorpayOrderId;

    @Column(length = 40)
    private String razorpayPaymentId;

    @Column(length = 40)
    private String razorpayRefundId;

    @Column(length = 500)
    private String note;

    @JdbcTypeCode(SqlTypes.TIMESTAMP_UTC)
    private Instant createdAt;

    @JdbcTypeCode(SqlTypes.TIMESTAMP_UTC)
    private Instant paidAt;

    @JdbcTypeCode(SqlTypes.TIMESTAMP_UTC)
    private Instant refundedAt;

    public Payment(User user, RaceSession session, Double amount, String razorpayOrderId) {
        this.user = user;
        this.session = session;
        this.amount = amount;
        this.razorpayOrderId = razorpayOrderId;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public boolean hasStatus(PaymentStatus wanted) {
        return status == wanted;
    }

    // the column holds 500 characters: a longer reason is cut, so saving never fails because of the text
    public void setNote(String note) {
        // a short note (or none) is saved as it came
        this.note = Optional.ofNullable(note)
                .filter(text -> text.length() > 500)
                .map(text -> text.substring(0, 500))
                .orElse(note);
    }
}
