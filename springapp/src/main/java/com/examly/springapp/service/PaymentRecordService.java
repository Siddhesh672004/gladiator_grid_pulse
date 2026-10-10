package com.examly.springapp.service;

import java.time.Instant;
import java.util.List;

import com.examly.springapp.dto.response.PaymentResponse;
import com.examly.springapp.dto.response.RevenueResponse;
import com.examly.springapp.model.Payment;
import com.examly.springapp.model.RaceSession;
import com.examly.springapp.model.User;

// every status change of a payment row happens here (Razorpay itself is called by RazorpayService)
public interface PaymentRecordService {

    Payment openPayment(User driver, RaceSession session, double amount, String razorpayOrderId);

    // CREATED -> PAID, only inside the booking transaction (RaceBookingServiceImpl.addRaceBooking)
    Payment markPaid(String razorpayOrderId, String razorpayPaymentId, User driver, Long sessionId);

    // PAID -> REFUND_PENDING, inside a reject / cancel transaction. old bookings have no payment -> nothing
    void requestRefund(Payment payment, String reason);

    // paid but no booking came of it: CREATED / FAILED -> REFUND_PENDING. false = nothing to refund (payment already used)
    boolean refundIfUnused(String razorpayOrderId, String razorpayPaymentId, String reason);

    void markRefunded(Long paymentId, String razorpayRefundId);

    void markRefundFailed(Long paymentId, String reason);

    void markFailed(Long paymentId);

    PaymentResponse retryRefund(Long paymentId);

    List<PaymentResponse> getAllPayments();

    List<Payment> findRefundPending();

    List<Payment> findUnpaidOlderThan(Instant cutoff);

    RevenueResponse getRevenueReport();
}
