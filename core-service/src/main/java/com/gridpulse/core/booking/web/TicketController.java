package com.gridpulse.core.booking.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gridpulse.core.booking.dto.TicketCheckResponse;
import com.gridpulse.core.booking.service.TicketService;
import com.gridpulse.web.security.AdminOnly;

import lombok.RequiredArgsConstructor;

// the gate: an admin types or scans the code of an e-ticket (the pdf itself: RaceBookingController)
@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    // always 200: valid true / false + a message for the gate
    @AdminOnly
    @GetMapping("/verify/{code}")
    public ResponseEntity<TicketCheckResponse> verify(@PathVariable String code) {
        return ResponseEntity.ok(ticketService.check(code));
    }
}
