package com.gridpulse.core.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gridpulse.core.dto.request.InternalBookingRequest;
import com.gridpulse.core.dto.request.RaceBookingRequest;
import com.gridpulse.core.dto.response.BookingQuote;
import com.gridpulse.core.dto.response.RaceBookingResponse;
import com.gridpulse.core.exceptions.ResourceNotFoundException;
import com.gridpulse.core.service.RaceBookingService;
import com.gridpulse.web.security.UserOnly;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// for payment-service only (InternalKeyFilter checks X-Internal-Key; the gateway never routes /internal/**)
// check + create carry the driver's own token, by-order only the key
@RestController
@RequestMapping("/internal/bookings")
@RequiredArgsConstructor
public class InternalBookingController {

    private final RaceBookingService raceBookingService;

    // before the razorpay order: every booking rule, the price and the snapshot for the payment
    @UserOnly
    @PostMapping("/check")
    public ResponseEntity<BookingQuote> check(@Valid @RequestBody RaceBookingRequest request) {
        return ResponseEntity.ok(raceBookingService.quote(request));
    }

    // after the money came in: 201 new booking, 200 = this order already has one; a broken rule -> its 4xx
    @UserOnly
    @PostMapping
    public ResponseEntity<RaceBookingResponse> create(@Valid @RequestBody InternalBookingRequest request) {
        var result = raceBookingService.createPaidBooking(request);
        return ResponseEntity.status(result.created() ? 201 : 200).body(result.booking());
    }

    // the reconciler: did this paid order become a booking? 404 = no
    @GetMapping("/by-order/{orderId}")
    public ResponseEntity<RaceBookingResponse> byOrder(@PathVariable String orderId) {
        return raceBookingService.findByOrder(orderId)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("No booking for order " + orderId));
    }
}
