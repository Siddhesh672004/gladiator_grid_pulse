package com.gridpulse.core.mapper;


import org.springframework.stereotype.Component;

import com.gridpulse.core.dto.request.RaceBookingRequest;
import com.gridpulse.core.dto.response.LiveStandingResponse;
import com.gridpulse.core.dto.response.RaceBookingResponse;
import com.gridpulse.core.model.RaceBooking;
import com.gridpulse.core.model.RaceSession;
import com.gridpulse.core.model.User;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RaceBookingMapper {

    private final UserMapper userMapper;
    private final SessionMapper sessionMapper;

    // the license is copied from the driver's profile, the booking keeps its own copy
    public RaceBooking toEntity(RaceBookingRequest request, User driver, RaceSession session) {
        return new RaceBooking(driver, session, request.carNumber(), request.carCategory(),
                driver.getDriverLicenseNumber(), request.comments());
    }

    public RaceBookingResponse toResponse(RaceBooking booking) {
        return toResponse(booking, null);
    }

    public RaceBookingResponse toResponse(RaceBooking booking, Integer waitlistPosition) {
        return new RaceBookingResponse(
                booking.getRaceBookingId(),
                userMapper.toResponse(booking.getUser()),
                sessionMapper.toResponse(booking.getSession()),
                booking.getStatus(),
                booking.getCarNumber(),
                booking.getCarCategory(),
                booking.getDriverLicenseNumber(),
                booking.getComments(),
                booking.getAdminNote(),
                booking.getTotalPrice(),
                booking.getTermsAcceptedAt(),
                booking.getLapCount(),
                booking.getLastLapAt(),
                booking.getRaceState(),
                booking.getFinalPosition(),
                booking.getGridPosition(),
                // the booking's own copy; a free race place of a competition has no payment
                booking.getPaymentStatus(),
                waitlistPosition);
    }

    // public board: only the driver's name, nothing private
    public LiveStandingResponse toLiveStanding(RaceBooking booking) {
        return new LiveStandingResponse(
                booking.getRaceBookingId(),
                userMapper.toSummary(booking.getUser()),
                booking.getCarNumber(),
                booking.getCarCategory(),
                booking.getLapCount(),
                booking.getLastLapAt(),
                booking.getRaceState(),
                booking.getFinalPosition(),
                booking.getGridPosition());
    }
}
