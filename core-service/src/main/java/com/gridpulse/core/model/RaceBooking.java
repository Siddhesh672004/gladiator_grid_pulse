package com.gridpulse.core.model;

import java.time.Instant;

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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// one car registered for one session, the race fields at the bottom are written only by race control
@Entity
@Getter
@Setter
@NoArgsConstructor
public class RaceBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long raceBookingId;

    @ManyToOne
    @JoinColumn(name = "userId")
    private User user;

    @ManyToOne
    @JoinColumn(name = "sessionId")
    private RaceSession session;

    private String status;

    // unique inside one session, rejected bookings don't count
    private Integer carNumber;

    private String carCategory;

    private String driverLicenseNumber;

    @Column(length = 500)
    private String comments;

    @Column(length = 500)
    private String adminNote;

    private Double totalPrice;

    @JdbcTypeCode(SqlTypes.TIMESTAMP_UTC)
    private Instant termsAcceptedAt;

    // the razorpay order of this booking + a copy of its payment status (payment-service owns the payment itself)
    // empty for bookings older than payments and for the free race place of a competition
    // varchar, not a mysql enum: a new status needs no table change
    @Column(length = 40, unique = true)
    private String paymentOrderId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 20)
    private PaymentStatus paymentStatus;

    private Integer lapCount = 0;
    @JdbcTypeCode(SqlTypes.TIMESTAMP_UTC)
    private Instant lastLapAt;
    private String raceState = "NOT_STARTED";
    private Integer finalPosition;
    private Integer gridPosition;

    public RaceBooking(User user, RaceSession session, Integer carNumber, String carCategory,
            String driverLicenseNumber, String comments) {
        this.user = user;
        this.session = session;
        this.carNumber = carNumber;
        this.carCategory = carCategory;
        this.driverLicenseNumber = driverLicenseNumber;
        this.comments = comments;
    }

    public RaceBooking(Long raceBookingId, User user, RaceSession session, Integer carNumber, String carCategory,
            String driverLicenseNumber, String comments) {
        this(user, session, carNumber, carCategory, driverLicenseNumber, comments);
        this.raceBookingId = raceBookingId;
    }
}
