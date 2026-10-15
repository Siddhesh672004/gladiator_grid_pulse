package com.examly.springapp.service;

import java.util.List;
import java.util.Optional;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.examly.springapp.config.CacheConfig;
import com.examly.springapp.dto.response.LiveStandingResponse;
import com.examly.springapp.exceptions.BookingException;
import com.examly.springapp.exceptions.ResourceNotFoundException;
import com.examly.springapp.mapper.RaceBookingMapper;
import com.examly.springapp.model.BookingStatus;
import com.examly.springapp.model.RaceBooking;
import com.examly.springapp.model.RaceSession;
import com.examly.springapp.model.SessionStatus;
import com.examly.springapp.repository.RaceBookingRepo;
import com.examly.springapp.repository.RaceSessionRepo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// practice control: the car rules are in RaceRules, this adds the session part
// first Start -> session LIVE, Finish -> COMPLETED, Reset -> back to SCHEDULED
@Slf4j
@Service
@RequiredArgsConstructor
public class RaceControlServiceImpl implements RaceControlService {

    private final RaceBookingRepo raceBookingRepo;
    private final RaceSessionRepo raceSessionRepo;
    private final RaceBookingService raceBookingService;
    private final RaceBookingMapper raceBookingMapper;

    @Override
    public List<LiveStandingResponse> getLiveStandings(Long sessionId) {
        findSession(sessionId); // 404 for an unknown session
        return toResponses(RaceRules.rank(raceBookingService.findApprovedCars(sessionId)));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = { CacheConfig.TRACKS, CacheConfig.TRACK }, allEntries = true)
    public LiveStandingResponse startRace(Long raceBookingId) {
        RaceBooking booking = findPracticeCar(raceBookingId);
        RaceRules.start(booking);
        RaceBooking saved = raceBookingRepo.save(booking);
        markLive(saved.getSession());
        log.info("Car #{} (booking {}) started", saved.getCarNumber(), raceBookingId);
        return raceBookingMapper.toLiveStanding(saved);
    }

    @Override
    @Transactional
    public LiveStandingResponse incrementLap(Long raceBookingId) {
        RaceBooking booking = findPracticeCar(raceBookingId);
        RaceRules.addLap(booking);
        RaceBooking saved = raceBookingRepo.save(booking);
        log.info("Car #{} (booking {}) completed lap {}", saved.getCarNumber(), raceBookingId, saved.getLapCount());
        return raceBookingMapper.toLiveStanding(saved);
    }

    @Override
    @Transactional
    public LiveStandingResponse retireCar(Long raceBookingId) {
        RaceBooking booking = findPracticeCar(raceBookingId);
        RaceRules.retire(booking);
        RaceBooking saved = raceBookingRepo.save(booking);
        log.info("Car #{} (booking {}) is RETIRED after {} lap(s)", saved.getCarNumber(), raceBookingId,
                saved.getLapCount());
        return raceBookingMapper.toLiveStanding(saved);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = { CacheConfig.TRACKS, CacheConfig.TRACK }, allEntries = true)
    public List<LiveStandingResponse> resetSession(Long sessionId) {
        RaceSession session = findSession(sessionId);
        checkPracticeStillOpen(session);

        List<RaceBooking> cars = raceBookingService.findApprovedCars(sessionId);
        // every car back to the start, then all saved at once
        raceBookingRepo.saveAll(cars.stream()
                .peek(RaceRules::reset)
                .toList());

        session.setStatus(SessionStatus.SCHEDULED);
        raceSessionRepo.save(session);
        log.info("Session {} reset ({} car(s))", sessionId, cars.size());
        return toResponses(RaceRules.rank(cars));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = { CacheConfig.TRACKS, CacheConfig.TRACK }, allEntries = true)
    public List<LiveStandingResponse> finishSession(Long sessionId) {
        RaceSession session = findSession(sessionId);
        checkPracticeStillOpen(session);
        boolean neverStarted = session.isScheduled();

        List<RaceBooking> standings = RaceRules.rank(raceBookingService.findApprovedCars(sessionId));
        int classified = RaceRules.classify(standings);
        raceBookingRepo.saveAll(standings);

        session.setStatus(SessionStatus.COMPLETED);
        raceSessionRepo.save(session);
        // finished without a single start: the drivers still waiting are rejected (and refunded) here
        Optional.of(neverStarted)
                .filter(Boolean::booleanValue)
                .ifPresent(notStarted -> raceBookingService.rejectWaitingBookings(sessionId, "The session is over"));
        log.info("Session {} finished: {} classified, {} retired", sessionId, classified,
                standings.size() - classified);
        return toResponses(standings);
    }

    private RaceBooking findPracticeCar(Long raceBookingId) {
        RaceBooking booking = raceBookingRepo.findById(raceBookingId)
                .orElseThrow(() -> new BookingException("Race booking with ID: " + raceBookingId + " not found."));
        // only an approved car goes on track
        Optional.of(booking)
                .filter(car -> BookingStatus.APPROVED.equals(car.getStatus()))
                .orElseThrow(() -> new BookingException("Car #" + booking.getCarNumber()
                        + " is not approved for this session"));
        checkPracticeStillOpen(booking.getSession());
        return booking;
    }

    private void checkPracticeStillOpen(RaceSession session) {
        // competition stages have their own control page
        Optional.of(session)
                .filter(RaceSession::isPractice)
                .orElseThrow(() -> new BookingException(
                        "This session belongs to a competition - use Competition Control"));
        // a completed or cancelled session is closed for good
        Optional.of(session)
                .filter(open -> open.getStatus() != SessionStatus.COMPLETED
                        && open.getStatus() != SessionStatus.CANCELLED)
                .orElseThrow(() -> new BookingException("This session is " + session.getStatus()
                        + " - race control is closed"));
    }

    // the first car on track makes the session LIVE, drivers still waiting are rejected
    private void markLive(RaceSession session) {
        // later starts leave the session as it is
        Optional.of(session)
                .filter(RaceSession::isScheduled)
                .ifPresent(scheduled -> {
                    scheduled.setStatus(SessionStatus.LIVE);
                    raceSessionRepo.save(scheduled);
                    raceBookingService.rejectWaitingBookings(scheduled.getSessionId(), "The session has started");
                });
    }

    private RaceSession findSession(Long sessionId) {
        return raceSessionRepo.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session with ID: " + sessionId + " not found."));
    }

    private List<LiveStandingResponse> toResponses(List<RaceBooking> bookings) {
        return bookings.stream()
                .map(raceBookingMapper::toLiveStanding)
                .toList();
    }
}
