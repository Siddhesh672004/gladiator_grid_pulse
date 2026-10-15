package com.gridpulse.payment.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
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
import jakarta.persistence.PrePersist;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// made BEFORE the driver pays (status CREATED), the booking only exists once the core saved it
// no relations: the driver and the session live in the core, a copy of what the admin pages show is kept here
// never deleted: the money history stays even when the booking is cancelled / deleted
@Entity
@Getter
@Setter
@NoArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentId;

    private Integer userId;

    private Long sessionId;

    // empty until the core saved the booking
    private Long bookingId;

    @Column(length = 100)
    private String driverName;

    @Column(length = 100)
    private String driverEmail;

    @Column(length = 20)
    private String driverMobile;

    @Column(length = 100)
    private String trackName;

    @Column(length = 100)
    private String trackLocation;

    private LocalDate sessionDate;

    private LocalTime startTime;

    private LocalTime endTime;

    // PRACTICE / COMPETITION, QUALIFYING / RACING: the admin page names the session with them
    @Column(length = 20)
    private String sessionType;

    @Column(length = 20)
    private String phase;

    @Column(length = 100)
    private String competitionName;

    // in rupees
    private Double amount;

    // varchar, not a mysql enum: a new status needs no table change
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
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

    // when the money came in (CAPTURED)
    @JdbcTypeCode(SqlTypes.TIMESTAMP_UTC)
    private Instant paidAt;

    @JdbcTypeCode(SqlTypes.TIMESTAMP_UTC)
    private Instant refundedAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public boolean hasStatus(PaymentStatus wanted) {
        return status == wanted;
    }

    // the column holds 500 characters: a longer reason is cut, so saving never fails because of the text
    public void setNote(String note) {
        this.note = Optional.ofNullable(note)
                .filter(text -> text.length() > 500)
                .map(text -> text.substring(0, 500))
                .orElse(note);
    }
}
