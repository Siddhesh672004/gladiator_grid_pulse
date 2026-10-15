package com.examly.springapp.service;

import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.examly.springapp.dto.response.ChartPoint;
import com.examly.springapp.dto.response.PageResponse;
import com.examly.springapp.dto.response.PaymentResponse;
import com.examly.springapp.dto.response.RevenueResponse;
import com.examly.springapp.exceptions.BookingException;
import com.examly.springapp.exceptions.ConflictException;
import com.examly.springapp.exceptions.ResourceNotFoundException;
import com.examly.springapp.mapper.PaymentMapper;
import com.examly.springapp.model.Payment;
import com.examly.springapp.model.PaymentStatus;
import com.examly.springapp.model.RaceSession;
import com.examly.springapp.model.User;
import com.examly.springapp.repository.MonthAmount;
import com.examly.springapp.repository.PaymentRepo;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// every status change: lock the row, check the status it starts from, change it (a double click and the refund job can't clash)
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentRecordServiceImpl implements PaymentRecordService {

    private final PaymentRepo paymentRepo;
    private final PaymentAuditService paymentAuditService;
    private final PaymentMapper paymentMapper;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public Payment openPayment(User driver, RaceSession session, double amount, String razorpayOrderId) {
        Payment saved = paymentRepo.save(new Payment(driver, session, amount, razorpayOrderId));
        paymentAuditService.record(razorpayOrderId, PaymentAuditService.ORDER_CREATED,
                "Order for " + rupees(amount) + " made for " + driver.getEmail());
        return saved;
    }

    // "paid" only counts together with the booking: joins the transaction of RaceBookingServiceImpl.addRaceBooking
    @Override
    @Transactional
    public Payment markPaid(String razorpayOrderId, String razorpayPaymentId, User driver, Long sessionId) {
        Payment payment = paymentRepo.findWithLockByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment for order " + razorpayOrderId + " not found."));
        // the driver's own payment, made for this session
        Optional.of(payment)
                .filter(order -> order.getUser().getUserId() == driver.getUserId())
                .orElseThrow(() -> new AccessDeniedException("This payment belongs to another driver"));
        Optional.of(payment)
                .filter(order -> order.getSession().getSessionId().equals(sessionId))
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
        payment.setStatus(PaymentStatus.PAID);
        payment.setRazorpayPaymentId(razorpayPaymentId);
        payment.setPaidAt(Instant.now());
        paymentAuditService.record(razorpayOrderId, PaymentAuditService.PAYMENT_RECEIVED,
                rupees(payment.getAmount()) + " received, Razorpay payment " + razorpayPaymentId);
        return paymentRepo.save(payment);
    }

    // joins the reject / cancel transaction: the refund and the booking change are saved together or not at all
    @Override
    @Transactional
    public void requestRefund(Payment payment, String reason) {
        // no payment = old booking, or the free race place of a competition: nothing to send back
        Optional.ofNullable(payment)
                .map(this::lockAndReadAgain)
                .filter(locked -> locked.hasStatus(PaymentStatus.PAID))
                .ifPresent(locked -> {
                    locked.setStatus(PaymentStatus.REFUND_PENDING);
                    locked.setNote(reason);
                    paymentRepo.save(locked);
                });
    }

    // the booking read this payment before we had the lock: read the row again, the refund job may have been here
    private Payment lockAndReadAgain(Payment payment) {
        Payment locked = lock(payment.getPaymentId());
        entityManager.refresh(locked, LockModeType.PESSIMISTIC_WRITE);
        return locked;
    }

    // runs after the booking transaction rolled back. PAID means another click did make a booking: nothing to refund
    // FAILED = the refund job gave the order up, but the driver paid after all
    @Override
    @Transactional
    public boolean refundIfUnused(String razorpayOrderId, String razorpayPaymentId, String reason) {
        // true only when a payment was found and no booking came out of it
        return paymentRepo.findWithLockByRazorpayOrderId(razorpayOrderId)
                .filter(payment -> payment.hasStatus(PaymentStatus.CREATED)
                        || payment.hasStatus(PaymentStatus.FAILED))
                .map(payment -> {
                    payment.setStatus(PaymentStatus.REFUND_PENDING);
                    payment.setRazorpayPaymentId(razorpayPaymentId);
                    payment.setPaidAt(Instant.now());
                    payment.setNote(reason);
                    return paymentRepo.save(payment);
                })
                .isPresent();
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
                    paymentRepo.save(payment);
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
                    paymentRepo.save(payment);
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
                    paymentRepo.save(payment);
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
        return paymentMapper.toResponse(paymentRepo.save(payment));
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

    private Payment lock(Long paymentId) {
        return paymentRepo.findWithLockByPaymentId(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment with ID: " + paymentId + " not found."));
    }

    // 650.0 -> "Rs.650"
    private static String rupees(double amount) {
        return "Rs." + new DecimalFormat("0.##").format(amount);
    }
}
