package com.gridpulse.payment.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.gridpulse.payment.client.BookingQuote;
import com.gridpulse.payment.client.CoreCalls;
import com.gridpulse.payment.client.InternalBookingRequest;
import com.gridpulse.payment.dto.request.PaymentConfirmRequest;
import com.gridpulse.payment.dto.request.RaceBookingRequest;
import com.gridpulse.payment.dto.response.ConfirmResult;
import com.gridpulse.payment.dto.response.PaymentOrderResponse;
import com.gridpulse.payment.exceptions.BookingException;
import com.gridpulse.payment.exceptions.CoreRefusedException;
import com.gridpulse.payment.exceptions.CoreUnavailableException;
import com.gridpulse.payment.exceptions.GridPulseException;
import com.gridpulse.payment.model.Payment;
import com.gridpulse.web.security.TokenUser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// the checkout saga: core check -> razorpay order -> the driver pays -> CAPTURED -> the core saves the booking -> PAID
// talks to Razorpay and the core: no @Transactional here (no row may stay locked while they answer)
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    static final String BEING_CONFIRMED =
            "Payment received. Your booking is being confirmed - check My Registrations in a minute.";

    private final CoreCalls coreCalls;
    private final PaymentRecordService paymentRecordService;
    private final PaymentAuditService paymentAuditService;
    private final RazorpayService razorpayService;

    @Override
    public PaymentOrderResponse createOrder(RaceBookingRequest request) {
        // every booking rule first (the core, with the driver's token), then the order
        BookingQuote quote = coreCalls.check(request);

        // razorpay wants paise: 650 rupees = 65000
        long amountPaise = Math.round(quote.price() * 100);
        String receipt = "gp-" + quote.userId() + "-" + quote.sessionId();
        String orderId = razorpayService.createOrder(amountPaise, receipt);

        paymentRecordService.openPayment(quote, orderId);
        return new PaymentOrderResponse(razorpayService.keyId(), orderId, amountPaise, "INR", quote.description(),
                quote.driverName(), quote.driverEmail(), quote.driverMobile());
    }

    @Override
    public ConfirmResult confirm(PaymentConfirmRequest request) {
        String orderId = request.razorpayOrderId();
        String paymentId = request.razorpayPaymentId();

        // the signature proves the answer really came from Razorpay
        Optional.of(request)
                .filter(paid -> razorpayService.isValidSignature(orderId, paymentId, paid.razorpaySignature()))
                .orElseThrow(() -> {
                    log.warn("Payment for order {} could not be verified (wrong signature)", orderId);
                    return new BookingException("The payment could not be verified. Nothing was booked.");
                });

        Payment payment = capture(request);
        return book(payment, request);
    }

    // CREATED -> CAPTURED. paid too late or for another session: the money goes back, the error goes to the driver
    // a second click on a used payment refunds nothing
    private Payment capture(PaymentConfirmRequest request) {
        String orderId = request.razorpayOrderId();
        try {
            return paymentRecordService.markCaptured(orderId, request.razorpayPaymentId(),
                    TokenUser.current().userId(), request.booking().sessionId());
        } catch (GridPulseException e) {
            boolean refunded = paymentRecordService.refundIfUnused(orderId, request.razorpayPaymentId(),
                    "Paid, but the booking was refused: " + e.getMessage());
            // only a refund that really started is written to the audit trail
            Optional.of(refunded)
                    .filter(Boolean::booleanValue)
                    .ifPresent(started -> paymentAuditService.record(orderId, PaymentAuditService.BOOKING_REFUSED,
                            e.getMessage()));
            throw e;
        }
    }

    // the core saves the booking: 2xx -> PAID, 4xx -> money back + the core's error, no answer -> 202 (job finishes)
    private ConfirmResult book(Payment payment, PaymentConfirmRequest request) {
        String orderId = payment.getRazorpayOrderId();
        try {
            JsonNode booking = coreCalls.createBooking(new InternalBookingRequest(request.booking(), orderId,
                    request.razorpayPaymentId(), payment.getAmount()));
            long bookingId = booking.path("raceBookingId").asLong();
            paymentRecordService.markPaid(orderId, bookingId);
            paymentAuditService.record(orderId, PaymentAuditService.BOOKING_CREATED, "Booking " + bookingId
                    + " (car #" + booking.path("carNumber").asText() + ") is waiting for approval");
            return ConfirmResult.booked(booking);
        } catch (CoreRefusedException e) {
            paymentRecordService.refundCaptured(orderId, "Paid, but the booking was refused: " + e.getMessage());
            paymentAuditService.record(orderId, PaymentAuditService.BOOKING_REFUSED, e.getMessage());
            throw e;
        } catch (CoreUnavailableException e) {
            log.warn("The core did not confirm the booking of order {}, the payment check finishes it", orderId);
            paymentAuditService.record(orderId, PaymentAuditService.CONFIRM_PENDING,
                    "Paid, the booking service did not answer - checked again in a minute");
            return ConfirmResult.pending(BEING_CONFIRMED);
        }
    }
}
