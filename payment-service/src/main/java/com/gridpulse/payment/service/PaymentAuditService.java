package com.gridpulse.payment.service;

import java.util.List;

import com.gridpulse.payment.dto.response.PaymentAuditResponse;

// the audit trail of the payments
public interface PaymentAuditService {

    String ORDER_CREATED = "ORDER_CREATED";
    String PAYMENT_RECEIVED = "PAYMENT_RECEIVED";
    String BOOKING_CREATED = "BOOKING_CREATED";
    String BOOKING_REFUSED = "BOOKING_REFUSED";
    // paid, but the core did not answer: RefundJob finishes it
    String CONFIRM_PENDING = "CONFIRM_PENDING";
    String REFUND_REQUESTED = "REFUND_REQUESTED";
    String REFUND_DONE = "REFUND_DONE";
    String REFUND_FAILED = "REFUND_FAILED";

    void record(String razorpayOrderId, String event, String detail);

    List<PaymentAuditResponse> getTrail(String razorpayOrderId);
}
