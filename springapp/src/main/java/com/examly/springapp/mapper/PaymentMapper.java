package com.examly.springapp.mapper;

import org.springframework.stereotype.Component;

import com.examly.springapp.dto.response.PaymentAuditResponse;
import com.examly.springapp.dto.response.PaymentResponse;
import com.examly.springapp.model.Payment;
import com.examly.springapp.model.PaymentAudit;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PaymentMapper {

    private final UserMapper userMapper;
    private final SessionMapper sessionMapper;

    public PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getPaymentId(),
                userMapper.toResponse(payment.getUser()),
                sessionMapper.toResponse(payment.getSession()),
                payment.getAmount(),
                payment.getStatus(),
                payment.getRazorpayOrderId(),
                payment.getRazorpayPaymentId(),
                payment.getRazorpayRefundId(),
                payment.getNote(),
                payment.getCreatedAt(),
                payment.getPaidAt(),
                payment.getRefundedAt());
    }

    public PaymentAuditResponse toAuditResponse(PaymentAudit audit) {
        return new PaymentAuditResponse(audit.getEvent(), audit.getDetail(), audit.getCreatedAt());
    }
}
