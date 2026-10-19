package com.gridpulse.core.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.gridpulse.core.config.properties.TicketProperties;
import com.gridpulse.core.dto.response.TicketCheckResponse;
import com.gridpulse.core.exceptions.BookingException;
import com.gridpulse.core.exceptions.ResourceNotFoundException;
import com.gridpulse.core.model.BookingStatus;
import com.gridpulse.core.model.RaceBooking;
import com.gridpulse.core.model.RaceSession;
import com.gridpulse.core.repository.RaceBookingRepo;
import com.gridpulse.common.ticket.TicketCodes;
import com.gridpulse.common.ticket.TicketData;
import com.gridpulse.common.ticket.TicketPdf;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// the code (TicketCodes) is the proof: only someone with TICKET_SECRET can make one, so no table of tickets is needed
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    // a laptop without TICKET_SECRET still gets tickets; prod refuses to start without it (ProdSettingsCheck)
    private static final String DEV_SECRET = "gridpulse-dev-ticket-secret-not-for-a-real-server";

    private final TicketProperties ticketProperties;
    private final RaceBookingRepo raceBookingRepo;
    private final UserService userService;

    private String secret;

    @PostConstruct
    public void init() {
        secret = Optional.ofNullable(ticketProperties.ticketSecret())
                .filter(value -> !value.isBlank())
                .orElseGet(() -> {
                    log.warn("TICKET_SECRET is not set - e-tickets use a fixed dev secret, set it on a real server");
                    return DEV_SECRET;
                });
    }

    @Override
    public String codeFor(Long bookingId) {
        return TicketCodes.sign(bookingId, secret);
    }

    @Override
    public byte[] ticketPdf(Long bookingId) {
        RaceBooking booking = raceBookingRepo.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Race booking with ID: " + bookingId + " not found."));
        userService.checkOwnerOrAdmin(booking.getUser().getUserId());
        Optional.of(booking)
                .filter(approved -> BookingStatus.APPROVED.equals(approved.getStatus()))
                .orElseThrow(() -> new BookingException("Only an approved booking has a ticket (this one is "
                        + booking.getStatus() + ")"));
        RaceSession session = booking.getSession();
        return TicketPdf.render(new TicketData(bookingId, codeFor(bookingId), booking.getUser().getUsername(),
                session.getTrack().getTrackName(), session.getTrack().getLocation(), session.getSessionDate(),
                session.getStartTime(), session.getEndTime(), booking.getCarNumber(), booking.getCarCategory()));
    }

    // a code we signed -> the booking as it is now (a rejected or deleted booking lets nobody in)
    @Override
    public TicketCheckResponse check(String code) {
        return TicketCodes.verify(code, secret)
                .map(bookingId -> raceBookingRepo.findById(bookingId)
                        .map(this::checked)
                        .orElseGet(() -> invalid("This booking no longer exists - no entry")))
                .orElseGet(() -> invalid("This is not a GridPulse ticket"));
    }

    private TicketCheckResponse checked(RaceBooking booking) {
        RaceSession session = booking.getSession();
        boolean approved = BookingStatus.APPROVED.equals(booking.getStatus());
        String message = approved
                ? "Valid ticket - car #" + booking.getCarNumber() + " may enter"
                : "This booking is " + booking.getStatus() + " - no entry";
        return new TicketCheckResponse(approved, booking.getRaceBookingId(), booking.getUser().getUsername(),
                booking.getCarNumber(), session.getTrack().getTrackName(), session.getSessionDate(),
                session.getStartTime(), booking.getStatus(), message);
    }

    private TicketCheckResponse invalid(String message) {
        return new TicketCheckResponse(false, null, null, null, null, null, null, null, message);
    }
}
