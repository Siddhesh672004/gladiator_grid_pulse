package com.examly.springapp.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.examly.springapp.dto.request.PaymentConfirmRequest;
import com.examly.springapp.dto.request.RaceBookingRequest;
import com.examly.springapp.dto.response.PaymentOrderResponse;
import com.examly.springapp.dto.response.RaceBookingResponse;
import com.examly.springapp.exceptions.BookingException;
import com.examly.springapp.exceptions.GridPulseException;
import com.examly.springapp.model.RaceSession;
import com.examly.springapp.model.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// checkout flow: only directs the work, the booking transaction itself is in RaceBookingServiceImpl
// talks to Razorpay: no @Transactional here and never call it inside one (no row may stay locked while Razorpay answers)
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final RaceBookingService raceBookingService;
    private final PaymentRecordService paymentRecordService;
    private final PaymentAuditService paymentAuditService;
    private final RazorpayService razorpayService;
    private final UserService userService;

    @Override
    public PaymentOrderResponse createOrder(RaceBookingRequest request) {
        RaceSession session = raceBookingService.checkCanBook(request);
        User driver = userService.getLoggedInUser();

        // razorpay wants paise: 650 rupees = 65000
        double amount = session.getPrice();
        long amountPaise = Math.round(amount * 100);
        String receipt = "gp-" + driver.getUserId() + "-" + session.getSessionId();
        String orderId = razorpayService.createOrder(amountPaise, receipt);

        paymentRecordService.openPayment(driver, session, amount, orderId);

        String description = session.getTrack().getTrackName() + " · " + session.getSessionDate() + " "
                + session.getStartTime() + "-" + session.getEndTime();
        return new PaymentOrderResponse(razorpayService.keyId(), orderId, amountPaise, "INR", description,
                driver.getUsername(), driver.getEmail(), driver.getMobileNumber());
    }

    @Override
    public RaceBookingResponse confirm(PaymentConfirmRequest request) {
        String orderId = request.razorpayOrderId();
        String paymentId = request.razorpayPaymentId();

        // the signature proves the answer really came from Razorpay
        Optional.of(request)
                .filter(paid -> razorpayService.isValidSignature(orderId, paymentId, paid.razorpaySignature()))
                .orElseThrow(() -> {
                    log.warn("Payment for order {} could not be verified (wrong signature)", orderId);
                    return new BookingException("The payment could not be verified. Nothing was booked.");
                });

        try {
            RaceBookingResponse booking = raceBookingService.addRaceBooking(request.booking(), orderId, paymentId);
            paymentAuditService.record(orderId, PaymentAuditService.BOOKING_CREATED,
                    "Booking " + booking.raceBookingId() + " (car #" + booking.carNumber() + ") is waiting for approval");
            return booking;
        } catch (GridPulseException e) {
            // booking rolled back but the money came in -> refund. a second click on a used payment refunds nothing
            boolean refunded = paymentRecordService.refundIfUnused(orderId, paymentId,
                    "Paid, but the booking was refused: " + e.getMessage());
            // only a refund that really started is written to the audit trail
            Optional.of(refunded)
                    .filter(Boolean::booleanValue)
                    .ifPresent(started -> paymentAuditService.record(orderId, PaymentAuditService.BOOKING_REFUSED,
                            e.getMessage()));
            throw e;
        }
    }
}
