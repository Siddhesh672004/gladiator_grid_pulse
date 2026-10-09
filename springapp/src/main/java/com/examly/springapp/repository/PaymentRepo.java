package com.examly.springapp.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.examly.springapp.model.Payment;
import com.examly.springapp.model.PaymentStatus;

import jakarta.persistence.LockModeType;

public interface PaymentRepo extends JpaRepository<Payment, Long> {

    // PESSIMISTIC_WRITE = select ... for update: a second request for the same payment (double click, refund job) waits
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Payment> findWithLockByRazorpayOrderId(String razorpayOrderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Payment> findWithLockByPaymentId(Long paymentId);

    List<Payment> findByStatus(PaymentStatus status);

    // the refund job looks at these
    List<Payment> findByStatusAndCreatedAtBefore(PaymentStatus status, Instant cutoff);

    List<Payment> findAllByOrderByPaymentIdDesc();

    // total money in one status, 0 when there is none
    @Query("select coalesce(sum(p.amount), 0) from Payment p where p.status = ?1")
    Double sumAmountByStatus(PaymentStatus status);

    long countByStatus(PaymentStatus status);

    // admin dashboard: money kept per month (paidAt is UTC)
    @Query("select month(p.paidAt) as month, sum(p.amount) as amount from Payment p"
            + " where p.status = ?1 and year(p.paidAt) = ?2 group by month(p.paidAt)")
    List<MonthAmount> sumPerMonth(PaymentStatus status, int year);
}
