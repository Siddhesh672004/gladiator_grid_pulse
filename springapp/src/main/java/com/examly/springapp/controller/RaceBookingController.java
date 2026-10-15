package com.examly.springapp.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.examly.springapp.dto.request.BookingStatusRequest;
import com.examly.springapp.dto.response.CarCategoryResponse;
import com.examly.springapp.dto.response.PageResponse;
import com.examly.springapp.dto.response.RaceBookingResponse;
import com.examly.springapp.security.annotation.AdminOnly;
import com.examly.springapp.security.annotation.LoggedIn;
import com.examly.springapp.security.annotation.UserOnly;
import com.examly.springapp.service.RaceBookingService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// approve / reject / cancel / list; a booking itself is only made through the payment (PaymentController)
@RestController
@RequestMapping("/api/race-booking")
@RequiredArgsConstructor
public class RaceBookingController {

    private final RaceBookingService raceBookingService;

    @LoggedIn
    @GetMapping("/car-categories")
    public ResponseEntity<List<CarCategoryResponse>> getCarCategories() {
        return ResponseEntity.ok(raceBookingService.getCarCategories());
    }

    @AdminOnly
    @GetMapping
    public ResponseEntity<List<RaceBookingResponse>> getAllRaceBookings() {
        return ResponseEntity.ok(raceBookingService.getAllRaceBookings());
    }

    // one page of the admin list, page counts from 0
    @AdminOnly
    @GetMapping("/page")
    public ResponseEntity<PageResponse<RaceBookingResponse>> getBookingPage(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(raceBookingService.getBookingPage(search, status, page, size));
    }

    @UserOnly
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<RaceBookingResponse>> getRaceBookingsByUserId(@PathVariable int userId) {
        return ResponseEntity.ok(raceBookingService.getRaceBookingsByUserId(userId));
    }

    @LoggedIn
    @GetMapping("/{id}")
    public ResponseEntity<RaceBookingResponse> getRaceBookingById(@PathVariable Long id) {
        return ResponseEntity.ok(raceBookingService.getRaceBookingById(id));
    }

    // approve / reject; 400 without a reason for a reject, 409 when the session is full
    @AdminOnly
    @PutMapping("/{id}")
    public ResponseEntity<RaceBookingResponse> updateRaceBooking(@PathVariable Long id,
            @Valid @RequestBody BookingStatusRequest request) {
        return ResponseEntity.ok(raceBookingService.updateRaceBooking(id, request));
    }

    @LoggedIn
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRaceBooking(@PathVariable Long id) {
        raceBookingService.deleteRaceBooking(id);
        return ResponseEntity.noContent().build();
    }
}
