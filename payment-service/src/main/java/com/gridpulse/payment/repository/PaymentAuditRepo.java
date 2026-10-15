package com.gridpulse.payment.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gridpulse.payment.model.PaymentAudit;

public interface PaymentAuditRepo extends JpaRepository<PaymentAudit, Long> {

    List<PaymentAudit> findByRazorpayOrderIdOrderByAuditIdAsc(String razorpayOrderId);
}
