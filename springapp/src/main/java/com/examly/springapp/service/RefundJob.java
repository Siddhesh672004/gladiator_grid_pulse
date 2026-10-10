package com.examly.springapp.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.examly.springapp.exceptions.GatewayException;
import com.examly.springapp.model.Payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// every 30 s: REFUND_PENDING -> Razorpay refund -> REFUNDED / REFUND_FAILED, and old unpaid orders are checked
// the reject click only writes REFUND_PENDING, this job does the slow part
@Slf4j
@Component
@RequiredArgsConstructor
public class RefundJob {

    private static final int UNPAID_MINUTES = 15;

    private final PaymentRecordService paymentRecordService;
    private final RazorpayService razorpayService;

    @Scheduled(fixedDelay = 30_000, initialDelay = 30_000)
    public void run() {
        // no razorpay keys = nothing to do
        Optional.of(razorpayService)
                .filter(RazorpayService::isConfigured)
                .ifPresent(razorpay -> refundAndCheck());
    }

    // waiting refunds first, then the orders nobody paid in time
    private void refundAndCheck() {
        paymentRecordService.findRefundPending().forEach(this::refund);
        Instant cutoff = Instant.now().minus(UNPAID_MINUTES, ChronoUnit.MINUTES);
        paymentRecordService.findUnpaidOlderThan(cutoff).forEach(this::checkUnpaid);
    }

    private void refund(Payment payment) {
        try {
            String refundId = razorpayService.refund(payment.getRazorpayPaymentId(), Math.round(payment.getAmount() * 100));
            paymentRecordService.markRefunded(payment.getPaymentId(), refundId);
        } catch (GatewayException e) {
            paymentRecordService.markRefundFailed(payment.getPaymentId(), e.getMessage());
        }
    }

    private void checkUnpaid(Payment payment) {
        try {
            // paid after all -> the money goes back, nobody paid -> the order is closed as failed
            Optional.ofNullable(razorpayService.findCapturedPayment(payment.getRazorpayOrderId()))
                    .ifPresentOrElse(
                            razorpayPaymentId -> paymentRecordService.refundIfUnused(payment.getRazorpayOrderId(),
                                    razorpayPaymentId, "Paid, but the booking was never confirmed"),
                            () -> paymentRecordService.markFailed(payment.getPaymentId()));
        } catch (GatewayException e) {
            // nothing changes, the next run asks again
            log.warn("Could not check unpaid order {}: {}", payment.getRazorpayOrderId(), e.getMessage());
        }
    }
}
