package com.gridpulse.core.booking.mapper;

import org.springframework.stereotype.Component;

import com.gridpulse.core.account.data.User;
import com.gridpulse.core.account.mapper.UserMapper;
import com.gridpulse.core.booking.data.RaceBooking;
import com.gridpulse.core.booking.dto.RaceBookingRequest;
import com.gridpulse.core.booking.dto.RaceBookingResponse;
import com.gridpulse.core.racecontrol.dto.LiveStandingResponse;
import com.gridpulse.core.session.data.RaceSession;
import com.gridpulse.core.session.mapper.SessionMapper;

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
