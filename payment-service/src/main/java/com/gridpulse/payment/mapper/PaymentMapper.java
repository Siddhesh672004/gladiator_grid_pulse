package com.gridpulse.payment.mapper;

import org.springframework.stereotype.Component;

import com.gridpulse.payment.dto.response.PaymentAuditResponse;
import com.gridpulse.payment.dto.response.PaymentResponse;
import com.gridpulse.payment.dto.response.SessionResponse;
import com.gridpulse.payment.dto.response.UserResponse;
import com.gridpulse.payment.model.Payment;
import com.gridpulse.payment.model.PaymentAudit;

// the same PaymentResponse json the core sent: driver + session built from the payment's copy, the rest null
@Component
public class PaymentMapper {

    public PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getPaymentId(),
                new UserResponse(payment.getUserId(), payment.getDriverEmail(), payment.getDriverName(),
                        payment.getDriverMobile(), null),
                new SessionResponse(payment.getSessionId(), null, payment.getTrackName(), payment.getTrackLocation(),
                        payment.getSessionType(), payment.getPhase(), null, payment.getCompetitionName(),
                        payment.getSessionDate(), payment.getStartTime(), payment.getEndTime(), null, null, null, null),
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
