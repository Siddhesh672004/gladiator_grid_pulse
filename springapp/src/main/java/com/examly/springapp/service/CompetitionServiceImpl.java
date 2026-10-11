package com.examly.springapp.service;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.examly.springapp.dto.CompetitionRule;
import com.examly.springapp.dto.request.CompetitionRequest;
import com.examly.springapp.dto.response.CompetitionResponse;
import com.examly.springapp.dto.response.SessionResponse;
import com.examly.springapp.exceptions.BookingException;
import com.examly.springapp.exceptions.ConflictException;
import com.examly.springapp.exceptions.ResourceNotFoundException;
import com.examly.springapp.mapper.CompetitionMapper;
import com.examly.springapp.model.Competition;
import com.examly.springapp.model.CompetitionPhase;
import com.examly.springapp.model.CompetitionStatus;
import com.examly.springapp.model.RaceSession;
import com.examly.springapp.model.RaceTrack;
import com.examly.springapp.model.SessionStatus;
import com.examly.springapp.model.SessionType;
import com.examly.springapp.repository.CompetitionRepo;
import com.examly.springapp.repository.RaceSessionRepo;
import com.examly.springapp.repository.RaceTrackRepo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// a competition = one parent row + two sessions (QUALIFYING and RACING) on the same track and day
@Slf4j
@Service
@RequiredArgsConstructor
public class CompetitionServiceImpl implements CompetitionService {

    private final CompetitionRepo competitionRepo;
    private final RaceSessionRepo raceSessionRepo;
    private final RaceTrackRepo raceTrackRepo;
    private final RaceSessionService raceSessionService;
    private final RaceBookingService raceBookingService;
    private final CompetitionMapper competitionMapper;

    @Override
    @Transactional
    public CompetitionResponse createCompetition(CompetitionRequest request) {
        // a name can be used once (any case)
        Optional.of(request.competitionName().trim())
                .filter(name -> !competitionRepo.existsByCompetitionNameIgnoreCase(name))
                .orElseThrow(() -> new ConflictException("A competition with this name already exists"));
        RaceTrack track = findTrack(request.trackId());
        CompetitionRule rule = RaceRules.ruleFor(track.getTrackCategory());

        // both slots must be free and the race comes after qualifying
        raceSessionService.checkSlot(track, request.competitionDate(), request.qualifyingStartTime(),
                request.qualifyingEndTime(), null);
        raceSessionService.checkSlot(track, request.competitionDate(), request.raceStartTime(),
                request.raceEndTime(), null);
        // no race start before the qualifying end
        Optional.of(request)
                .filter(times -> !times.raceStartTime().isBefore(times.qualifyingEndTime()))
                .orElseThrow(() -> new BookingException("The race must start after qualifying has ended"));

        Competition competition = competitionRepo.save(competitionMapper.toEntity(request, track, rule));

        // the entry fee is paid when booking qualifying, the race place is free
        raceSessionRepo.save(newStage(competition, CompetitionPhase.QUALIFYING, request.qualifyingStartTime(),
                request.qualifyingEndTime(), request.entryFee()));
        raceSessionRepo.save(newStage(competition, CompetitionPhase.RACING, request.raceStartTime(),
                request.raceEndTime(), 0.0));

        log.info("Competition {} '{}' created on '{}' {} ({} rule: {} / {} laps, {} cars)",
                competition.getCompetitionId(), competition.getCompetitionName(), track.getTrackName(),
                competition.getCompetitionDate(), rule.trackCategory(), rule.qualifyingLaps(), rule.raceLaps(),
                rule.maxCars());
        return toResponse(competition);
    }

    @Override
    public List<CompetitionResponse> getAllCompetitions() {
        return competitionRepo.findAllByOrderByCompetitionDateDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public CompetitionResponse getCompetitionById(Long competitionId) {
        return toResponse(findCompetition(competitionId));
    }

    @Override
    @Transactional
    public CompetitionResponse cancelCompetition(Long competitionId) {
        Competition competition = findCompetition(competitionId);
        // once qualifying has started it is too late to cancel
        Optional.of(competition)
                .filter(upcoming -> upcoming.getStatus() == CompetitionStatus.UPCOMING)
                .orElseThrow(() -> new BookingException("This competition is " + competition.getStatus()
                        + " - only an UPCOMING competition can be cancelled"));
        competition.setStatus(CompetitionStatus.CANCELLED);
        competitionRepo.save(competition);

        cancelStage(competition, CompetitionPhase.QUALIFYING);
        cancelStage(competition, CompetitionPhase.RACING);
        log.info("Competition {} cancelled", competitionId);
        return toResponse(competition);
    }

    @Override
    public CompetitionRule getRuleForTrack(Long trackId) {
        return RaceRules.ruleFor(findTrack(trackId).getTrackCategory());
    }

    @Override
    public RaceSession findStage(Competition competition, CompetitionPhase phase) {
        return raceSessionRepo.findByCompetitionCompetitionIdAndPhase(competition.getCompetitionId(), phase)
                .orElseThrow(() -> new ResourceNotFoundException("The " + phase + " session of competition "
                        + competition.getCompetitionId() + " is missing."));
    }

    @Override
    public Competition findCompetition(Long competitionId) {
        return competitionRepo.findById(competitionId)
                .orElseThrow(() -> new ResourceNotFoundException("Competition with ID: " + competitionId
                        + " not found."));
    }

    private RaceSession newStage(Competition competition, CompetitionPhase phase, LocalTime startTime,
            LocalTime endTime, Double price) {
        RaceSession stage = new RaceSession(competition.getTrack(), competition.getCompetitionDate(), startTime,
                endTime, price, competition.getMaxCars());
        stage.setSessionType(SessionType.COMPETITION);
        stage.setCompetition(competition);
        stage.setPhase(phase);
        return stage;
    }

    private void cancelStage(Competition competition, CompetitionPhase phase) {
        RaceSession stage = findStage(competition, phase);
        stage.setStatus(SessionStatus.CANCELLED);
        raceSessionRepo.save(stage);
        raceBookingService.rejectAllBookings(stage.getSessionId(), "Competition cancelled by GridPulse");
    }

    private RaceTrack findTrack(Long trackId) {
        return raceTrackRepo.findById(trackId)
                .orElseThrow(() -> new BookingException("Track with ID: " + trackId + " not found."));
    }

    private CompetitionResponse toResponse(Competition competition) {
        SessionResponse qualifying = raceSessionService.getSessionById(
                findStage(competition, CompetitionPhase.QUALIFYING).getSessionId());
        SessionResponse race = raceSessionService.getSessionById(
                findStage(competition, CompetitionPhase.RACING).getSessionId());
        return competitionMapper.toResponse(competition, qualifying, race);
    }
}
