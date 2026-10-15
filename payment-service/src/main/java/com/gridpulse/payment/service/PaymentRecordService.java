package com.gridpulse.payment.service;

import java.time.Instant;
import java.util.List;

import com.gridpulse.common.event.BookingRefundRequested;
import com.gridpulse.payment.client.BookingQuote;
import com.gridpulse.payment.dto.response.PageResponse;
import com.gridpulse.payment.dto.response.PaymentResponse;
import com.gridpulse.payment.dto.response.RevenueResponse;
import com.gridpulse.payment.model.Payment;

// every status change of a payment row happens here, each one also goes out as PaymentStatusChanged (outbox)
// (Razorpay itself is called by RazorpayService, the core by CoreCalls)
public interface PaymentRecordService {

    Payment openPayment(BookingQuote quote, String razorpayOrderId);

    // CREATED -> CAPTURED: the driver's own order for this session, not too late, not used yet
    Payment markCaptured(String razorpayOrderId, String razorpayPaymentId, int userId, Long sessionId);

    // CAPTURED -> PAID: the core saved booking bookingId
    void markPaid(String razorpayOrderId, Long bookingId);

    // CAPTURED -> REFUND_PENDING: the core refused the booking, or never saved it
    void refundCaptured(String razorpayOrderId, String reason);

    // paid but no booking came of it: CREATED / FAILED -> REFUND_PENDING. false = nothing to refund (payment already used)
    boolean refundIfUnused(String razorpayOrderId, String razorpayPaymentId, String reason);

    // the core rejected / cancelled a paid booking (BookingRefundRequested): PAID -> REFUND_PENDING
    void refundForBooking(BookingRefundRequested request);

    void markRefunded(Long paymentId, String razorpayRefundId);

    void markRefundFailed(Long paymentId, String reason);

    void markFailed(Long paymentId);

    PaymentResponse retryRefund(Long paymentId);

    List<PaymentResponse> getAllPayments();

    // admin list: newest first, search = driver name, email or track, status = a PaymentStatus or All
    PageResponse<PaymentResponse> getPaymentPage(String search, String status, int page, int size);

    List<Payment> findRefundPending();

    List<Payment> findUnpaidOlderThan(Instant cutoff);

    List<Payment> findCapturedOlderThan(Instant cutoff);

    RevenueResponse getRevenueReport();
}
