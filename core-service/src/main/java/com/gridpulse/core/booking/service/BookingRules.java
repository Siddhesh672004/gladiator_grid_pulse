package com.gridpulse.core.booking.service;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import org.springframework.stereotype.Component;

import com.gridpulse.core.account.data.User;
import com.gridpulse.core.booking.data.BookingStatus;
import com.gridpulse.core.booking.data.PaymentStatus;
import com.gridpulse.core.booking.data.RaceBooking;
import com.gridpulse.core.booking.data.RaceBookingRepo;
import com.gridpulse.core.platform.exception.BookingException;
import com.gridpulse.core.platform.exception.ConflictException;
import com.gridpulse.core.platform.exception.DuplicateCarNumberException;
import com.gridpulse.core.session.data.RaceSession;

import lombok.RequiredArgsConstructor;

// the rules of a booking: each check only reads and throws the answer the driver / admin sees
// no transaction of its own, a check runs inside the caller's (the session lock included)
@Component
@RequiredArgsConstructor
public class BookingRules {

    // a rejected booking holds no car number and no time slot
    private static final List<String> HOLDS_A_PLACE = List.of(BookingStatus.PENDING, BookingStatus.APPROVED,
            BookingStatus.WAITLISTED);

    // one rule of a session: when it is broken + the message for it
    private record SessionRule(Predicate<RaceSession> broken, Function<RaceSession, String> message) {
    }

    // a driver books only a session that waits for its start, on an active track
    private static final List<SessionRule> OPEN_FOR_BOOKING = List.of(
            new SessionRule(RaceSession::isRacing,
                    session -> "Race places come from qualifying - book the qualifying session instead"),
            new SessionRule(session -> !session.isScheduled(),
                    session -> "This session is " + session.getStatus() + " - booking is closed"),
            new SessionRule(RaceSession::hasStarted,
                    session -> "Booking is closed: this session has already started"),
            new SessionRule(session -> !session.getTrack().isActive(),
                    session -> "This track is not available right now"));

    // approve / reject only before the session starts
    private static final List<SessionRule> BOOKINGS_CAN_CHANGE = List.of(
            new SessionRule(RaceSession::isRacing,
                    session -> "Race places come from qualifying and cannot be changed"),
            new SessionRule(session -> !session.isScheduled(),
                    session -> "This session is " + session.getStatus() + " - its bookings can no longer be changed"));

    private final RaceBookingRepo raceBookingRepo;

    public void checkNewBooking(RaceBooking booking) {
        RaceSession session = booking.getSession();
        checkProfileComplete(booking.getUser());
        checkOpenForBooking(session);
        checkDriverIsFree(booking.getUser(), session);
        checkCarNumberFree(booking);
    }

    private void checkProfileComplete(User driver) {
        Optional.of(driver)
                .filter(User::isProfileComplete)
                .orElseThrow(() -> new BookingException("Please complete your profile before booking a race"));
    }

    // one driver, one session at a time on any track. back to back (13:30 end,
    // 13:30 start) is fine
    private void checkDriverIsFree(User driver, RaceSession session) {
        raceBookingRepo.findByUserUserIdAndSessionSessionDateAndStatusIn(driver.getUserId(),
                        session.getSessionDate(), HOLDS_A_PLACE).stream()
                .map(RaceBooking::getSession)
                .filter(taken -> session.getStartTime().isBefore(taken.getEndTime())
                        && taken.getStartTime().isBefore(session.getEndTime()))
                .findFirst()
                .ifPresent(taken -> {
                    throw new ConflictException(
                            "This driver is already registered for an overlapping session on this date");
                });
    }

    private void checkOpenForBooking(RaceSession session) {
        checkSessionRules(OPEN_FOR_BOOKING, session);
    }

    public void checkBookingCanChange(RaceSession session) {
        checkSessionRules(BOOKINGS_CAN_CHANGE, session);
    }

    // rules are tried from the top, the first broken one is the answer
    private void checkSessionRules(List<SessionRule> rules, RaceSession session) {
        rules.stream()
                .filter(rule -> rule.broken().test(session))
                .map(rule -> rule.message().apply(session))
                .findFirst()
                .ifPresent(message -> {
                    throw new BookingException(message);
                });
    }

    public void checkSeatFree(RaceSession session) {
        Optional.of(session)
                .filter(open -> !isFull(open))
                .orElseThrow(() -> new ConflictException("Session is full (" + session.getMaxCars() + " cars)"));
    }

    // only Approved bookings take a seat
    public boolean isFull(RaceSession session) {
        return raceBookingRepo.countBySessionSessionIdAndStatus(session.getSessionId(), BookingStatus.APPROVED)
                >= session.getMaxCars();
    }

    // a reject needs a reason of at least 5 characters
    public void checkReason(String note) {
        String reason = Optional.ofNullable(note)
                .orElseThrow(() -> new BookingException("Please write a reason for rejecting this booking"));
        Optional.of(reason)
                .filter(text -> text.length() >= 5)
                .orElseThrow(() -> new BookingException("Reason must be at least 5 characters"));
    }

    // a rejected booking does not hold its car number
    private void checkCarNumberFree(RaceBooking booking) {
        Optional.of(booking)
                .filter(car -> !raceBookingRepo.existsBySessionSessionIdAndCarNumberAndStatusNot(
                        car.getSession().getSessionId(), car.getCarNumber(), BookingStatus.REJECTED))
                .orElseThrow(() -> new DuplicateCarNumberException("Car number " + booking.getCarNumber()
                        + " is already registered for this session"));
    }

    public void checkCarNumberStillFree(RaceBooking booking) {
        Optional.of(booking)
                .filter(car -> !raceBookingRepo.existsBySessionSessionIdAndCarNumberAndStatusNotAndRaceBookingIdNot(
                        car.getSession().getSessionId(), car.getCarNumber(), BookingStatus.REJECTED,
                        car.getRaceBookingId()))
                .orElseThrow(() -> new DuplicateCarNumberException("Car number " + booking.getCarNumber()
                        + " was taken by another booking in the meantime"));
    }

    public void checkCanBeDeleted(RaceBooking booking) {
        RaceSession session = booking.getSession();
        Optional.of(session)
                .filter(stage -> !stage.isRacing())
                .orElseThrow(() -> new BookingException("Race places come from qualifying and cannot be removed"));
        // an approved car of a session that already ran is part of the results
        Optional.of(booking)
                .filter(car -> BookingStatus.APPROVED.equals(car.getStatus()) && !session.isScheduled())
                .ifPresent(car -> {
                    throw new BookingException("This session is " + session.getStatus() + " - its results are kept");
                });
    }

    // a refunded booking can't be approved again (no payment status = a free race place)
    public void checkMoneyStillHere(RaceBooking booking) {
        Optional.ofNullable(booking.getPaymentStatus())
                .filter(status -> status != PaymentStatus.PAID)
                .ifPresent(status -> {
                    throw new BookingException(
                            "The payment of this booking was refunded - the driver has to book again");
                });
    }

    // only GridPulse puts a booking on the waitlist, and a waitlisted one only moves up by itself (or is rejected)
    public void checkWaitlistChange(String oldStatus, String newStatus) {
        Optional.of(newStatus)
                .filter(BookingStatus.WAITLISTED::equals)
                .ifPresent(status -> {
                    throw new BookingException("Waitlisted is set by GridPulse when the session is full");
                });
        // waitlisted = the session is full: approving it is the same clash as approving into a full session
        Optional.of(oldStatus)
                .filter(BookingStatus.WAITLISTED::equals)
                .filter(waiting -> BookingStatus.APPROVED.equals(newStatus))
                .ifPresent(waiting -> {
                    throw new ConflictException(
                            "Session is full - a waitlisted booking moves up by itself when a seat opens");
                });
        Optional.of(oldStatus)
                .filter(BookingStatus.WAITLISTED::equals)
                .filter(waiting -> !BookingStatus.REJECTED.equals(newStatus))
                .ifPresent(waiting -> {
                    throw new BookingException(
                            "A waitlisted booking can only be rejected - it moves up by itself when a seat opens");
                });
    }
}
