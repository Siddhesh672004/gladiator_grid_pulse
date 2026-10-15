package com.gridpulse.payment.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gridpulse.payment.dto.request.PaymentConfirmRequest;
import com.gridpulse.payment.dto.request.RaceBookingRequest;
import com.gridpulse.payment.dto.response.ConfirmResult;
import com.gridpulse.payment.dto.response.MessageResponse;
import com.gridpulse.payment.dto.response.PageResponse;
import com.gridpulse.payment.dto.response.PaymentAuditResponse;
import com.gridpulse.payment.dto.response.PaymentOrderResponse;
import com.gridpulse.payment.dto.response.PaymentResponse;
import com.gridpulse.payment.dto.response.RevenueResponse;
import com.gridpulse.payment.security.annotation.AdminOnly;
import com.gridpulse.payment.security.annotation.UserOnly;
import com.gridpulse.payment.service.PaymentAuditService;
import com.gridpulse.payment.service.PaymentRecordService;
import com.gridpulse.payment.service.PaymentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// checkout: order -> the driver pays -> confirm; the booking itself is saved by the core (/internal/bookings)
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentRecordService paymentRecordService;
    private final PaymentAuditService paymentAuditService;

    // step 1 of "Pay & Register": booking rules first, then a Razorpay order; 503 when payments are not set up
    @UserOnly
    @PostMapping("/order")
    public ResponseEntity<PaymentOrderResponse> createOrder(@Valid @RequestBody RaceBookingRequest request) {
        return ResponseEntity.status(201).body(paymentService.createOrder(request));
    }

    // step 2: the driver paid -> 201 + the Pending booking; refused -> its error + a refund;
    // the core did not answer -> 202 + "being confirmed"
    @UserOnly
    @PostMapping("/confirm")
    public ResponseEntity<Object> confirm(@Valid @RequestBody PaymentConfirmRequest request) {
        ConfirmResult result = paymentService.confirm(request);
        return result.confirmed()
                ? ResponseEntity.status(201).body(result.booking())
                : ResponseEntity.accepted().body(new MessageResponse(result.message()));
    }

    @AdminOnly
    @GetMapping
    public ResponseEntity<List<PaymentResponse>> getAllPayments() {
        return ResponseEntity.ok(paymentRecordService.getAllPayments());
    }

    // one page of the admin list, page counts from 0
    @AdminOnly
    @GetMapping("/page")
    public ResponseEntity<PageResponse<PaymentResponse>> getPaymentPage(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(paymentRecordService.getPaymentPage(search, status, page, size));
    }

    @AdminOnly
    @GetMapping("/summary")
    public ResponseEntity<RevenueResponse> getSummary() {
        return ResponseEntity.ok(paymentRecordService.getRevenueReport());
    }

    @AdminOnly
    @GetMapping("/audit")
    public ResponseEntity<List<PaymentAuditResponse>> getTrail(@RequestParam String orderId) {
        return ResponseEntity.ok(paymentAuditService.getTrail(orderId));
    }

    @AdminOnly
    @PostMapping("/{paymentId}/retry-refund")
    public ResponseEntity<PaymentResponse> retryRefund(@PathVariable Long paymentId) {
        return ResponseEntity.ok(paymentRecordService.retryRefund(paymentId));
    }
}
