package com.examly.springapp.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.examly.springapp.dto.response.PaymentAuditResponse;
import com.examly.springapp.mapper.PaymentMapper;
import com.examly.springapp.model.PaymentAudit;
import com.examly.springapp.repository.PaymentAuditRepo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentAuditServiceImpl implements PaymentAuditService {

    private final PaymentAuditRepo paymentAuditRepo;
    private final PaymentMapper paymentMapper;

    // REQUIRES_NEW: the audit line stays even when the booking rolls back
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String razorpayOrderId, String event, String detail) {
        paymentAuditRepo.save(new PaymentAudit(razorpayOrderId, event, detail));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentAuditResponse> getTrail(String razorpayOrderId) {
        return paymentAuditRepo.findByRazorpayOrderIdOrderByAuditIdAsc(razorpayOrderId).stream()
                .map(paymentMapper::toAuditResponse)
                .toList();
    }
}
