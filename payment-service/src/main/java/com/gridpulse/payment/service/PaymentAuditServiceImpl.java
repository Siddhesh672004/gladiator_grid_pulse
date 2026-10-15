package com.gridpulse.payment.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.gridpulse.payment.dto.response.PaymentAuditResponse;
import com.gridpulse.payment.mapper.PaymentMapper;
import com.gridpulse.payment.model.PaymentAudit;
import com.gridpulse.payment.repository.PaymentAuditRepo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentAuditServiceImpl implements PaymentAuditService {

    private final PaymentAuditRepo paymentAuditRepo;
    private final PaymentMapper paymentMapper;

    // REQUIRES_NEW: the audit line stays even when the caller rolls back
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
