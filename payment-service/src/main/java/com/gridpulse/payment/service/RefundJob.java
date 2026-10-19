package com.gridpulse.payment.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.function.Consumer;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.gridpulse.payment.client.CoreCalls;
import com.gridpulse.payment.exceptions.CoreUnavailableException;
import com.gridpulse.payment.exceptions.GatewayException;
import com.gridpulse.payment.model.Payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// every 30 s: confirms the core never answered (CAPTURED) are finished, REFUND_PENDING -> Razorpay refund ->
// REFUNDED / REFUND_FAILED, and old unpaid orders are checked. the clicks only change statuses, this job does the slow part
// gridpulse.jobs.refund.enabled=false switches it off (more instances: only one may refund)
@Slf4j
@Component
@ConditionalOnProperty(name = "gridpulse.jobs.refund.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class RefundJob {

    private static final int UNPAID_MINUTES = 15;
    // a confirm gives up on the core after about 16 s, so a minute old CAPTURED payment is nobody's work any more
    private static final int UNCONFIRMED_MINUTES = 1;

    private final PaymentRecordService paymentRecordService;
    private final PaymentAuditService paymentAuditService;
    private final RazorpayService razorpayService;
    private final CoreCalls coreCalls;

    @Scheduled(fixedDelay = 30_000, initialDelay = 30_000)
    public void run() {
        // first: a refund this finds is sent in the same run
        Instant unconfirmed = Instant.now().minus(UNCONFIRMED_MINUTES, ChronoUnit.MINUTES);
        paymentRecordService.findCapturedOlderThan(unconfirmed).forEach(payment -> isolated(payment, this::reconcile));
        // no razorpay keys = nothing to refund or check
        Optional.of(razorpayService)
                .filter(RazorpayService::isConfigured)
                .ifPresent(razorpay -> refundAndCheck());
    }

    // one broken payment must not stop the others: it keeps its status and the next run tries it again
    private void isolated(Payment payment, Consumer<Payment> work) {
        try {
            work.accept(payment);
        } catch (RuntimeException e) {
            log.warn("Refund job skipped order {}: {}", payment.getRazorpayOrderId(), e.getClass().getSimpleName());
        }
    }

    // did the core save the booking after all? yes -> PAID, no -> the money goes back, core still down -> next run
    private void reconcile(Payment payment) {
        String orderId = payment.getRazorpayOrderId();
        try {
            coreCalls.findByOrder(orderId).ifPresentOrElse(booking -> {
                long bookingId = booking.path("raceBookingId").asLong();
                paymentRecordService.markPaid(orderId, bookingId);
                paymentAuditService.record(orderId, PaymentAuditService.BOOKING_CREATED,
                        "Booking " + bookingId + " found by the payment check");
            }, () -> {
                paymentRecordService.refundCaptured(orderId, "Paid, but the booking was never confirmed");
                paymentAuditService.record(orderId, PaymentAuditService.BOOKING_REFUSED,
                        "The booking service never saved the booking");
            });
        } catch (CoreUnavailableException e) {
            log.warn("Could not check order {} with the core: {}", orderId, e.getMessage());
        }
    }

    // waiting refunds first, then the orders nobody paid in time
    private void refundAndCheck() {
        paymentRecordService.findRefundPending().forEach(payment -> isolated(payment, this::refund));
        Instant cutoff = Instant.now().minus(UNPAID_MINUTES, ChronoUnit.MINUTES);
        paymentRecordService.findUnpaidOlderThan(cutoff).forEach(payment -> isolated(payment, this::checkUnpaid));
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
