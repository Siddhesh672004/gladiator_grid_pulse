package com.examly.springapp.mapper;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.examly.springapp.dto.request.RaceBookingRequest;
import com.examly.springapp.dto.response.LiveStandingResponse;
import com.examly.springapp.dto.response.RaceBookingResponse;
import com.examly.springapp.model.Payment;
import com.examly.springapp.model.RaceBooking;
import com.examly.springapp.model.RaceSession;
import com.examly.springapp.model.User;

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
                // a free race place of a competition has no payment
                Optional.ofNullable(booking.getPayment()).map(Payment::getStatus).orElse(null));
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
