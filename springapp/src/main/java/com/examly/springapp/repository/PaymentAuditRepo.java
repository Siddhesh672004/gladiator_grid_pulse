package com.examly.springapp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.examly.springapp.model.PaymentAudit;

public interface PaymentAuditRepo extends JpaRepository<PaymentAudit, Long> {

    List<PaymentAudit> findByRazorpayOrderIdOrderByAuditIdAsc(String razorpayOrderId);
}
