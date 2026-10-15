package com.examly.springapp.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.examly.springapp.dto.request.PaymentConfirmRequest;
import com.examly.springapp.dto.request.RaceBookingRequest;
import com.examly.springapp.dto.response.PageResponse;
import com.examly.springapp.dto.response.PaymentAuditResponse;
import com.examly.springapp.dto.response.PaymentOrderResponse;
import com.examly.springapp.dto.response.PaymentResponse;
import com.examly.springapp.dto.response.RaceBookingResponse;
import com.examly.springapp.dto.response.RevenueResponse;
import com.examly.springapp.security.annotation.AdminOnly;
import com.examly.springapp.security.annotation.UserOnly;
import com.examly.springapp.service.PaymentAuditService;
import com.examly.springapp.service.PaymentRecordService;
import com.examly.springapp.service.PaymentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// checkout: order -> the driver pays -> confirm; the booking transaction itself is in RaceBookingServiceImpl
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

    // step 2: the driver paid -> a Pending booking; a refused booking gets a refund
    @UserOnly
    @PostMapping("/confirm")
    public ResponseEntity<RaceBookingResponse> confirm(@Valid @RequestBody PaymentConfirmRequest request) {
        return ResponseEntity.status(201).body(paymentService.confirm(request));
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
