package com.examly.springapp.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.examly.springapp.dto.request.SessionRequest;
import com.examly.springapp.dto.response.PageResponse;
import com.examly.springapp.dto.response.SessionResponse;
import com.examly.springapp.exceptions.BookingException;
import com.examly.springapp.exceptions.ConflictException;
import com.examly.springapp.exceptions.ResourceNotFoundException;
import com.examly.springapp.mapper.SessionMapper;
import com.examly.springapp.model.RaceSession;
import com.examly.springapp.model.RaceTrack;
import com.examly.springapp.model.SessionStatus;
import com.examly.springapp.model.SessionType;
import com.examly.springapp.repository.RaceSessionRepo;
import com.examly.springapp.repository.RaceTrackRepo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// sessions are created by the admin, drivers only book them
@Slf4j
@Service
@RequiredArgsConstructor
public class RaceSessionServiceImpl implements RaceSessionService {

    private static final int MONTHS_AHEAD = 12;
    private static final int MIN_MINUTES = 30;

    private final RaceSessionRepo raceSessionRepo;
    private final RaceTrackRepo raceTrackRepo;
    private final RaceBookingService raceBookingService;
    private final SessionMapper sessionMapper;

    @Override
    public SessionResponse addPracticeSession(SessionRequest request) {
        RaceTrack track = findTrack(request.trackId());
        checkSlot(track, request.sessionDate(), request.startTime(), request.endTime(), null);

        RaceSession saved = raceSessionRepo.save(sessionMapper.toEntity(request, track));
        log.info("Practice session {} created: '{}' {} {}-{}, price {}, max {} cars", saved.getSessionId(),
                track.getTrackName(), saved.getSessionDate(), saved.getStartTime(), saved.getEndTime(),
                saved.getPrice(), saved.getMaxCars());
        return toResponse(saved);
    }

    @Override
    public SessionResponse updatePracticeSession(Long sessionId, SessionRequest request) {
        RaceSession session = findSession(sessionId);
        checkIsPractice(session);
        checkIsScheduled(session);

        RaceTrack track = findTrack(request.trackId());
        checkChangedTimes(session, request);
        checkSlotOnTrack(track, request.sessionDate(), request.startTime(), request.endTime(), sessionId);
        checkMaxCarsNotBelowApproved(session, request.maxCars());

        sessionMapper.updateEntity(session, request, track);
        RaceSession saved = raceSessionRepo.save(session);
        log.info("Session {} updated", sessionId);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public SessionResponse cancelSession(Long sessionId) {
        RaceSession session = findSession(sessionId);
        checkIsPractice(session);
        checkIsScheduled(session);

        session.setStatus(SessionStatus.CANCELLED);
        RaceSession saved = raceSessionRepo.save(session);
        raceBookingService.rejectAllBookings(sessionId, "Session cancelled by GridPulse");
        log.info("Session {} cancelled", sessionId);
        return toResponse(saved);
    }

    @Override
    public List<SessionResponse> getAllSessions() {
        return toResponses(raceSessionRepo.findAllByOrderBySessionDateDescStartTimeAsc());
    }

    @Override
    public PageResponse<SessionResponse> getSessionPage(String search, String status, String type, int page,
            int size) {
        Sort newestFirst = Sort.by(Sort.Direction.DESC, "sessionDate", "startTime", "sessionId");
        Page<RaceSession> sessions = raceSessionRepo.searchPage(Paging.enumFilter(SessionStatus.class, status, "Status"),
                Paging.enumFilter(SessionType.class, type, "Type"), Paging.likePattern(search),
                Paging.pageable(page, size, newestFirst));
        // seats of the whole page in one query, not one per row
        Map<Long, Long> cars = raceBookingService.countApprovedCars(sessions.map(RaceSession::getSessionId).getContent());
        return Paging.toResponse(sessions,
                session -> sessionMapper.toResponse(session, cars.getOrDefault(session.getSessionId(), 0L)));
    }

    @Override
    public SessionResponse getSessionById(Long sessionId) {
        return toResponse(findSession(sessionId));
    }

    @Override
    public List<SessionResponse> getSessionsForTrackOnDate(Long trackId, LocalDate date) {
        return toResponses(raceSessionRepo.findByTrackTrackIdAndSessionDateAndStatusNotOrderByStartTime(trackId,
                date, SessionStatus.CANCELLED));
    }

    @Override
    public void checkSlot(RaceTrack track, LocalDate date, LocalTime startTime, LocalTime endTime,
            Long ignoreSessionId) {
        checkDateInRange(date);
        checkNotPassed(date, startTime);
        checkLongEnough(startTime, endTime);
        checkSlotOnTrack(track, date, startTime, endTime, ignoreSessionId);
    }

    // an old session stays editable: a time rule only counts for the part the admin changed
    private void checkChangedTimes(RaceSession old, SessionRequest request) {
        boolean newDate = !request.sessionDate().equals(old.getSessionDate());
        boolean newStart = !request.startTime().equals(old.getStartTime());
        boolean newEnd = !request.endTime().equals(old.getEndTime());
        Optional.of(request)
                .filter(changed -> newDate)
                .ifPresent(changed -> checkDateInRange(changed.sessionDate()));
        Optional.of(request)
                .filter(changed -> newDate || newStart)
                .ifPresent(changed -> checkNotPassed(changed.sessionDate(), changed.startTime()));
        Optional.of(request)
                .filter(changed -> newStart || newEnd)
                .ifPresent(changed -> checkLongEnough(changed.startTime(), changed.endTime()));
    }

    // a session can be planned up to 12 months ahead
    private void checkDateInRange(LocalDate date) {
        Optional.of(date)
                .filter(day -> !day.isAfter(RaceSession.today().plusMonths(MONTHS_AHEAD)))
                .orElseThrow(() -> new BookingException("Date must be within the next " + MONTHS_AHEAD + " months"));
    }

    // only today can be too late, India time
    private void checkNotPassed(LocalDate date, LocalTime startTime) {
        LocalDateTime now = RaceSession.now().truncatedTo(ChronoUnit.MINUTES);
        Optional.of(startTime)
                .filter(start -> date.equals(now.toLocalDate()) && start.isBefore(now.toLocalTime()))
                .ifPresent(start -> {
                    throw new BookingException("Start Time has already passed");
                });
    }

    // an end before the start gets its own message in checkSlotOnTrack
    private void checkLongEnough(LocalTime startTime, LocalTime endTime) {
        boolean tooShort = Duration.between(startTime, endTime).toMinutes() < MIN_MINUTES;
        Optional.of(endTime)
                .filter(end -> end.isAfter(startTime) && tooShort)
                .ifPresent(end -> {
                    throw new BookingException("A session must be at least " + MIN_MINUTES + " minutes long");
                });
    }

    private void checkSlotOnTrack(RaceTrack track, LocalDate date, LocalTime startTime, LocalTime endTime,
            Long ignoreSessionId) {
        Optional.of(track)
                .filter(RaceTrack::isActive)
                .orElseThrow(() -> new BookingException("Track '" + track.getTrackName()
                        + "' is inactive - activate it first"));
        Optional.of(date)
                .filter(day -> !day.isBefore(RaceSession.today()))
                .orElseThrow(() -> new BookingException("Date cannot be in the past"));
        Optional.of(endTime)
                .filter(end -> end.isAfter(startTime))
                .orElseThrow(() -> new BookingException("End Time must be after Start Time"));
        // start and end inside the opening hours of the track
        Optional.of(track)
                .filter(open -> !startTime.isBefore(open.getOpenTime()) && !endTime.isAfter(open.getCloseTime()))
                .orElseThrow(() -> new BookingException("Session must be within the track's operating hours ("
                        + track.getOpenTime() + " - " + track.getCloseTime() + ")"));
        checkNoOverlap(track, date, startTime, endTime, ignoreSessionId);
    }

    // the first other session on the track that same day that overlaps -> 409
    private void checkNoOverlap(RaceTrack track, LocalDate date, LocalTime startTime, LocalTime endTime,
            Long ignoreSessionId) {
        raceSessionRepo.findByTrackTrackIdAndSessionDateAndStatusNotOrderByStartTime(track.getTrackId(), date,
                        SessionStatus.CANCELLED).stream()
                .filter(other -> !other.getSessionId().equals(ignoreSessionId))
                .filter(other -> startTime.isBefore(other.getEndTime()) && other.getStartTime().isBefore(endTime))
                .findFirst()
                .ifPresent(other -> {
                    throw new ConflictException("This time overlaps another session on this track ("
                            + other.getStartTime() + " - " + other.getEndTime() + ")");
                });
    }

    // a competition stage is managed from its competition
    private void checkIsPractice(RaceSession session) {
        Optional.of(session)
                .filter(RaceSession::isPractice)
                .orElseThrow(() -> new BookingException("This session belongs to the competition '"
                        + session.getCompetition().getCompetitionName() + "' - open the competition to manage it"));
    }

    private void checkIsScheduled(RaceSession session) {
        Optional.of(session)
                .filter(RaceSession::isScheduled)
                .orElseThrow(() -> new BookingException("This session is " + session.getStatus()
                        + " - only a SCHEDULED session can be changed"));
    }

    // approved cars keep their seat
    private void checkMaxCarsNotBelowApproved(RaceSession session, Integer newMaxCars) {
        long approved = raceBookingService.countApprovedCars(session.getSessionId());
        Optional.of(newMaxCars)
                .filter(maxCars -> maxCars >= approved)
                .orElseThrow(() -> new BookingException("Max Cars cannot be less than the " + approved
                        + " car(s) already approved"));
    }

    private RaceSession findSession(Long sessionId) {
        return raceSessionRepo.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session with ID: " + sessionId + " not found."));
    }

    private RaceTrack findTrack(Long trackId) {
        return raceTrackRepo.findById(trackId)
                .orElseThrow(() -> new BookingException("Track with ID: " + trackId + " not found."));
    }

    private SessionResponse toResponse(RaceSession session) {
        return sessionMapper.toResponse(session, raceBookingService.countApprovedCars(session.getSessionId()));
    }

    private List<SessionResponse> toResponses(List<RaceSession> sessions) {
        return sessions.stream()
                .map(this::toResponse)
                .toList();
    }
}
