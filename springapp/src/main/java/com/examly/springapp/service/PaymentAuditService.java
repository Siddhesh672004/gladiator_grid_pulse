package com.examly.springapp.service;

import java.util.List;

import com.examly.springapp.dto.response.PaymentAuditResponse;

// the audit trail of the payments
public interface PaymentAuditService {

    String ORDER_CREATED = "ORDER_CREATED";
    String PAYMENT_RECEIVED = "PAYMENT_RECEIVED";
    String BOOKING_CREATED = "BOOKING_CREATED";
    String BOOKING_REFUSED = "BOOKING_REFUSED";
    String REFUND_DONE = "REFUND_DONE";
    String REFUND_FAILED = "REFUND_FAILED";

    void record(String razorpayOrderId, String event, String detail);

    List<PaymentAuditResponse> getTrail(String razorpayOrderId);
}
