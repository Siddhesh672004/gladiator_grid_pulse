package com.gridpulse.payment.service;

import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;

import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gridpulse.common.event.BookingRefundRequested;
import com.gridpulse.common.event.PaymentStatusChanged;
import com.gridpulse.common.event.Topics;
import com.gridpulse.common.outbox.EventPublisher;
import com.gridpulse.payment.client.BookingQuote;
import com.gridpulse.payment.dto.response.ChartPoint;
import com.gridpulse.payment.dto.response.PageResponse;
import com.gridpulse.payment.dto.response.PaymentResponse;
import com.gridpulse.payment.dto.response.RevenueResponse;
import com.gridpulse.payment.exceptions.BookingException;
import com.gridpulse.payment.exceptions.ConflictException;
import com.gridpulse.payment.exceptions.ResourceNotFoundException;
import com.gridpulse.payment.mapper.PaymentMapper;
import com.gridpulse.payment.model.Payment;
import com.gridpulse.payment.model.PaymentStatus;
import com.gridpulse.payment.repository.MonthAmount;
import com.gridpulse.payment.repository.PaymentRepo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// every status change: lock the row, check the status it starts from, change it (a double click, the refund job and
// a kafka event can't clash), and the PaymentStatusChanged event goes into the outbox in the same transaction
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentRecordServiceImpl implements PaymentRecordService {

    private final PaymentRepo paymentRepo;
    private final PaymentAuditService paymentAuditService;
    private final PaymentMapper paymentMapper;
    private final EventPublisher eventPublisher;

    @Override
    @Transactional
    public Payment openPayment(BookingQuote quote, String razorpayOrderId) {
        Payment payment = new Payment();
        payment.setUserId(quote.userId());
        payment.setSessionId(quote.sessionId());
        payment.setDriverName(quote.driverName());
        payment.setDriverEmail(quote.driverEmail());
        payment.setDriverMobile(quote.driverMobile());
        payment.setTrackName(quote.trackName());
        payment.setTrackLocation(quote.trackLocation());
        payment.setSessionDate(quote.sessionDate());
        payment.setStartTime(quote.startTime());
        payment.setEndTime(quote.endTime());
        payment.setSessionType(quote.sessionType());
        payment.setPhase(quote.phase());
        payment.setCompetitionName(quote.competitionName());
        payment.setAmount(quote.price());
        payment.setRazorpayOrderId(razorpayOrderId);
        Payment saved = paymentRepo.save(payment);
        paymentAuditService.record(razorpayOrderId, PaymentAuditService.ORDER_CREATED,
                "Order for " + rupees(quote.price()) + " made for " + quote.driverEmail());
        return saved;
    }

    @Override
    @Transactional
    public Payment markCaptured(String razorpayOrderId, String razorpayPaymentId, int userId, Long sessionId) {
        Payment payment = lockOrder(razorpayOrderId);
        // the driver's own payment, made for this session
        Optional.of(payment)
                .filter(order -> Objects.equals(order.getUserId(), userId))
                .orElseThrow(() -> new AccessDeniedException("This payment belongs to another driver"));
        Optional.of(payment)
                .filter(order -> Objects.equals(order.getSessionId(), sessionId))
                .orElseThrow(() -> new BookingException("This payment was made for another session"));
        // paid after the order had timed out: no booking, confirm sends the money back
        Optional.of(payment)
                .filter(order -> !order.hasStatus(PaymentStatus.FAILED))
                .orElseThrow(() -> new ConflictException("This payment came in too late. It will be refunded."));
        // one payment makes one booking
        Optional.of(payment)
                .filter(order -> order.hasStatus(PaymentStatus.CREATED))
                .orElseThrow(() -> new ConflictException(
                        "This payment was already used (it is " + payment.getStatus() + ")"));
        payment.setStatus(PaymentStatus.CAPTURED);
        payment.setRazorpayPaymentId(razorpayPaymentId);
        payment.setPaidAt(Instant.now());
        paymentAuditService.record(razorpayOrderId, PaymentAuditService.PAYMENT_RECEIVED,
                rupees(payment.getAmount()) + " received, Razorpay payment " + razorpayPaymentId);
        return changed(payment);
    }

    @Override
    @Transactional
    public void markPaid(String razorpayOrderId, Long bookingId) {
        Payment payment = lockOrder(razorpayOrderId);
        // a refund the core asked for while we waited keeps its status, it only learns the booking id
        payment.setBookingId(bookingId);
        Optional.of(payment)
                .filter(captured -> captured.hasStatus(PaymentStatus.CAPTURED))
                .ifPresentOrElse(captured -> {
                    captured.setStatus(PaymentStatus.PAID);
                    changed(captured);
                }, () -> paymentRepo.save(payment));
    }

    @Override
    @Transactional
    public void refundCaptured(String razorpayOrderId, String reason) {
        Optional.of(lockOrder(razorpayOrderId))
                .filter(payment -> payment.hasStatus(PaymentStatus.CAPTURED))
                .ifPresent(payment -> {
                    payment.setStatus(PaymentStatus.REFUND_PENDING);
                    payment.setNote(reason);
                    changed(payment);
                });
    }

    // runs when the confirm failed before anything was captured. PAID / CAPTURED means another click is using it
    // FAILED = the refund job gave the order up, but the driver paid after all
    @Override
    @Transactional
    public boolean refundIfUnused(String razorpayOrderId, String razorpayPaymentId, String reason) {
        return paymentRepo.findWithLockByRazorpayOrderId(razorpayOrderId)
                .filter(payment -> payment.hasStatus(PaymentStatus.CREATED)
                        || payment.hasStatus(PaymentStatus.FAILED))
                .map(payment -> {
                    payment.setStatus(PaymentStatus.REFUND_PENDING);
                    payment.setRazorpayPaymentId(razorpayPaymentId);
                    payment.setPaidAt(Instant.now());
                    payment.setNote(reason);
                    return changed(payment);
                })
                .isPresent();
    }

    // joins the listener's transaction (with the inbox row). CAPTURED too: the core may reject a booking before
    // the confirm (or the job) marked its payment PAID
    @Override
    @Transactional
    public void refundForBooking(BookingRefundRequested request) {
        paymentRepo.findWithLockByRazorpayOrderId(request.orderId())
                .ifPresentOrElse(payment -> refundBooked(payment, request),
                        // a booking paid before phase 5: its payment is still in appdb (docker/sql/phase5-copy-payments.sql)
                        () -> log.warn("Refund asked for order {} (booking {}), but there is no such payment here",
                                request.orderId(), request.bookingId()));
    }

    // any other status: already on its way back, or never paid
    private void refundBooked(Payment payment, BookingRefundRequested request) {
        Optional.of(payment)
                .filter(paid -> paid.hasStatus(PaymentStatus.PAID) || paid.hasStatus(PaymentStatus.CAPTURED))
                .ifPresentOrElse(paid -> {
                    paid.setStatus(PaymentStatus.REFUND_PENDING);
                    paid.setBookingId(request.bookingId());
                    paid.setNote(request.reason());
                    changed(paid);
                    paymentAuditService.record(paid.getRazorpayOrderId(), PaymentAuditService.REFUND_REQUESTED,
                            request.reason());
                }, () -> log.debug("Refund for order {} skipped, the payment is {}", request.orderId(),
                        payment.getStatus()));
    }

    @Override
    @Transactional
    public void markRefunded(Long paymentId, String razorpayRefundId) {
        // only a payment that is still waiting for its refund
        Optional.of(lock(paymentId))
                .filter(payment -> payment.hasStatus(PaymentStatus.REFUND_PENDING))
                .ifPresent(payment -> {
                    payment.setStatus(PaymentStatus.REFUNDED);
                    payment.setRazorpayRefundId(razorpayRefundId);
                    payment.setRefundedAt(Instant.now());
                    changed(payment);
                    paymentAuditService.record(payment.getRazorpayOrderId(), PaymentAuditService.REFUND_DONE,
                            rupees(payment.getAmount()) + " refunded, Razorpay refund " + razorpayRefundId);
                });
    }

    @Override
    @Transactional
    public void markRefundFailed(Long paymentId, String reason) {
        // only a payment that is still waiting for its refund
        Optional.of(lock(paymentId))
                .filter(payment -> payment.hasStatus(PaymentStatus.REFUND_PENDING))
                .ifPresent(payment -> {
                    payment.setStatus(PaymentStatus.REFUND_FAILED);
                    payment.setNote(reason);
                    changed(payment);
                    paymentAuditService.record(payment.getRazorpayOrderId(), PaymentAuditService.REFUND_FAILED,
                            reason);
                });
    }

    @Override
    @Transactional
    public void markFailed(Long paymentId) {
        // an order nobody paid in time
        Optional.of(lock(paymentId))
                .filter(payment -> payment.hasStatus(PaymentStatus.CREATED))
                .ifPresent(payment -> {
                    payment.setStatus(PaymentStatus.FAILED);
                    payment.setNote("Not paid in time");
                    changed(payment);
                });
    }

    @Override
    @Transactional
    public PaymentResponse retryRefund(Long paymentId) {
        Payment payment = lock(paymentId);
        // only a refund that failed can be asked for again
        Optional.of(payment)
                .filter(failed -> failed.hasStatus(PaymentStatus.REFUND_FAILED))
                .orElseThrow(() -> new BookingException("Only a failed refund can be tried again (this payment is "
                        + payment.getStatus() + ")"));
        payment.setStatus(PaymentStatus.REFUND_PENDING);
        log.info("Refund of payment {} will be tried again", paymentId);
        return paymentMapper.toResponse(changed(payment));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PaymentResponse> getPaymentPage(String search, String status, int page, int size) {
        return Paging.toResponse(paymentRepo.searchPage(Paging.enumFilter(PaymentStatus.class, status, "Status"),
                Paging.likePattern(search), Paging.pageable(page, size, Sort.by(Sort.Direction.DESC, "paymentId"))),
                paymentMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getAllPayments() {
        return paymentRepo.findAllByOrderByPaymentIdDesc().stream()
                .map(paymentMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payment> findRefundPending() {
        return paymentRepo.findByStatus(PaymentStatus.REFUND_PENDING);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payment> findUnpaidOlderThan(Instant cutoff) {
        return paymentRepo.findByStatusAndCreatedAtBefore(PaymentStatus.CREATED, cutoff);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payment> findCapturedOlderThan(Instant cutoff) {
        return paymentRepo.findByStatusAndPaidAtBefore(PaymentStatus.CAPTURED, cutoff);
    }

    // one transaction: all the sums read one snapshot (MySQL's normal level), so a refund in between can't be counted twice
    @Override
    @Transactional(readOnly = true)
    public RevenueResponse getRevenueReport() {
        double collected = paymentRepo.sumAmountByStatus(PaymentStatus.PAID);
        double refunded = paymentRepo.sumAmountByStatus(PaymentStatus.REFUNDED);
        double inProgress = paymentRepo.sumAmountByStatus(PaymentStatus.REFUND_PENDING)
                + paymentRepo.sumAmountByStatus(PaymentStatus.REFUND_FAILED);
        long paidBookings = paymentRepo.countByStatus(PaymentStatus.PAID);

        double[] months = MonthAmount.twelveMonths(
                paymentRepo.sumPerMonth(PaymentStatus.PAID, LocalDate.now().getYear()));
        // one chart point per month, Jan to Dec
        List<ChartPoint> monthlyRevenue = IntStream.range(0, 12)
                .mapToObj(month -> new ChartPoint(MonthAmount.MONTHS[month], months[month]))
                .toList();
        return new RevenueResponse(collected, refunded, inProgress, paidBookings, monthlyRevenue);
    }

    // saved + told to the others (the core copies the status onto the booking, notification emails refunds)
    private Payment changed(Payment payment) {
        Payment saved = paymentRepo.save(payment);
        eventPublisher.publish(Topics.PAYMENT, saved.getRazorpayOrderId(), new PaymentStatusChanged(
                saved.getRazorpayOrderId(), saved.getBookingId(), saved.getStatus().name(), saved.getAmount(),
                saved.getDriverEmail(), saved.getDriverName(), saved.getTrackName(), saved.getSessionDate(),
                saved.getNote()));
        return saved;
    }

    private Payment lockOrder(String razorpayOrderId) {
        return paymentRepo.findWithLockByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment for order " + razorpayOrderId + " not found."));
    }

    private Payment lock(Long paymentId) {
        return paymentRepo.findWithLockByPaymentId(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment with ID: " + paymentId + " not found."));
    }

    // 650.0 -> "Rs.650"
    private static String rupees(double amount) {
        return "Rs." + new DecimalFormat("0.##").format(amount);
    }
}
