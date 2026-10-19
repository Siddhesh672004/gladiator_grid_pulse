package com.gridpulse.core.competition.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gridpulse.core.booking.data.BookingStatus;
import com.gridpulse.core.booking.data.RaceBooking;
import com.gridpulse.core.booking.data.RaceBookingRepo;
import com.gridpulse.core.booking.mapper.RaceBookingMapper;
import com.gridpulse.core.booking.service.RaceBookingService;
import com.gridpulse.core.competition.data.Competition;
import com.gridpulse.core.competition.data.CompetitionPhase;
import com.gridpulse.core.competition.data.CompetitionRepo;
import com.gridpulse.core.competition.data.CompetitionStatus;
import com.gridpulse.core.competition.dto.CompetitionResponse;
import com.gridpulse.core.platform.config.CacheConfig;
import com.gridpulse.core.platform.exception.BookingException;
import com.gridpulse.core.racecontrol.dto.LiveStandingResponse;
import com.gridpulse.core.racecontrol.service.LiveBoardEvents;
import com.gridpulse.core.racecontrol.service.RaceRules;
import com.gridpulse.core.session.data.RaceSession;
import com.gridpulse.core.session.data.RaceSessionRepo;
import com.gridpulse.core.session.data.SessionStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// every step: check the status -> change the cars -> move the stage + competition status
// what one car may do (start, lap, retire, ranking) is in RaceRules, same as practice
@Slf4j
@Service
@RequiredArgsConstructor
public class CompetitionControlServiceImpl implements CompetitionControlService {

    private final CompetitionRepo competitionRepo;
    private final RaceSessionRepo raceSessionRepo;
    private final RaceBookingRepo raceBookingRepo;
    private final RaceBookingService raceBookingService;
    private final CompetitionService competitionService;
    private final RaceBookingMapper raceBookingMapper;
    private final LiveBoardEvents liveBoardEvents;

    @Override
    @Transactional
    @CacheEvict(cacheNames = { CacheConfig.TRACKS, CacheConfig.TRACK, CacheConfig.COMPETITIONS }, allEntries = true)
    public CompetitionResponse startQualifying(Long competitionId) {
        Competition competition = competitionService.findCompetition(competitionId);
        checkStatus(competition, CompetitionStatus.UPCOMING, "Start Qualifying");

        RaceSession qualifying = competitionService.findStage(competition, CompetitionPhase.QUALIFYING);
        List<RaceBooking> cars = raceBookingService.findApprovedCars(qualifying.getSessionId());
        // no qualifying without at least one approved car
        Optional.of(cars)
                .filter(approved -> !approved.isEmpty())
                .orElseThrow(() -> new BookingException(
                        "No approved drivers yet - approve at least one booking first"));
        // every car starts, then all saved at once
        raceBookingRepo.saveAll(cars.stream()
                .peek(RaceRules::start)
                .toList());

        // still Pending bookings are rejected now
        raceBookingService.rejectWaitingBookings(qualifying.getSessionId(), "Qualifying has started");
        moveTo(competition, CompetitionStatus.QUALIFYING, qualifying, SessionStatus.LIVE);
        log.info("Competition {}: qualifying started with {} car(s)", competitionId, cars.size());
        return competitionService.getCompetitionById(competitionId);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = { CacheConfig.TRACKS, CacheConfig.TRACK, CacheConfig.COMPETITIONS,
            CacheConfig.LEADERBOARD }, allEntries = true)
    public CompetitionResponse finishQualifying(Long competitionId) {
        Competition competition = competitionService.findCompetition(competitionId);
        checkStatus(competition, CompetitionStatus.QUALIFYING, "Finish Qualifying");

        RaceSession qualifying = competitionService.findStage(competition, CompetitionPhase.QUALIFYING);
        List<RaceBooking> result = RaceRules.rank(raceBookingService.findApprovedCars(qualifying.getSessionId()));
        RaceRules.classify(result);
        raceBookingRepo.saveAll(result);

        // grid = qualifying result: one race place per qualifier, best first, retired cars at the back
        RaceSession race = competitionService.findStage(competition, CompetitionPhase.RACING);
        // grid 1, 2 .. in result order
        IntStream.range(0, result.size())
                .mapToObj(i -> newRacePlace(result.get(i), race, i + 1))
                .forEach(raceBookingRepo::save);

        moveTo(competition, CompetitionStatus.QUALIFYING_COMPLETED, qualifying, SessionStatus.COMPLETED);
        // the race board now has its grid
        liveBoardEvents.changed(race.getSessionId(), "GRID");
        log.info("Competition {}: qualifying finished, grid of {} car(s) created", competitionId, result.size());
        return competitionService.getCompetitionById(competitionId);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = { CacheConfig.TRACKS, CacheConfig.TRACK, CacheConfig.COMPETITIONS }, allEntries = true)
    public CompetitionResponse startRace(Long competitionId) {
        Competition competition = competitionService.findCompetition(competitionId);
        checkStatus(competition, CompetitionStatus.QUALIFYING_COMPLETED, "Start Race");

        RaceSession race = competitionService.findStage(competition, CompetitionPhase.RACING);
        List<RaceBooking> grid = raceBookingService.findApprovedCars(race.getSessionId());
        // every car on the grid starts, then all saved at once
        raceBookingRepo.saveAll(grid.stream()
                .peek(RaceRules::start)
                .toList());

        moveTo(competition, CompetitionStatus.RACING, race, SessionStatus.LIVE);
        log.info("Competition {}: race started with {} car(s)", competitionId, grid.size());
        return competitionService.getCompetitionById(competitionId);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = { CacheConfig.TRACKS, CacheConfig.TRACK, CacheConfig.COMPETITIONS,
            CacheConfig.LEADERBOARD }, allEntries = true)
    public CompetitionResponse finishRace(Long competitionId) {
        Competition competition = competitionService.findCompetition(competitionId);
        checkStatus(competition, CompetitionStatus.RACING, "Finish Race");

        RaceSession race = competitionService.findStage(competition, CompetitionPhase.RACING);
        List<RaceBooking> result = RaceRules.rank(raceBookingService.findApprovedCars(race.getSessionId()));
        int classified = RaceRules.classify(result);
        raceBookingRepo.saveAll(result);

        moveTo(competition, CompetitionStatus.COMPLETED, race, SessionStatus.COMPLETED);
        log.info("Competition {}: race finished, {} classified, {} retired", competitionId, classified,
                result.size() - classified);
        return competitionService.getCompetitionById(competitionId);
    }

    @Override
    @Transactional
    public LiveStandingResponse addLap(Long raceBookingId) {
        RaceBooking booking = findRunningCar(raceBookingId);
        RaceRules.addLap(booking);

        // the last lap of the stage finishes the car
        Optional.of(booking)
                .filter(car -> RaceRules.lapsOf(car) >= car.getSession().lapLimit())
                .ifPresent(car -> car.setRaceState(RaceRules.FINISHED));
        RaceBooking saved = raceBookingRepo.save(booking);
        liveBoardEvents.changed(saved.getSession().getSessionId(), "LAP");
        log.info("Car #{} (booking {}) completed lap {}", saved.getCarNumber(), raceBookingId, saved.getLapCount());
        return raceBookingMapper.toLiveStanding(saved);
    }

    @Override
    @Transactional
    public LiveStandingResponse retireCar(Long raceBookingId) {
        RaceBooking booking = findRunningCar(raceBookingId);
        RaceRules.retire(booking);
        RaceBooking saved = raceBookingRepo.save(booking);
        liveBoardEvents.changed(saved.getSession().getSessionId(), "RETIRE");
        log.info("Car #{} (booking {}) is RETIRED after {} lap(s)", saved.getCarNumber(), raceBookingId,
                saved.getLapCount());
        return raceBookingMapper.toLiveStanding(saved);
    }

    private void checkStatus(Competition competition, CompetitionStatus needed, String button) {
        // each button works in one competition status only
        Optional.of(competition)
                .filter(current -> current.getStatus() == needed)
                .orElseThrow(() -> new BookingException("'" + button + "' is only possible when the competition is "
                        + needed + " - it is " + competition.getStatus()));
    }

    private void moveTo(Competition competition, CompetitionStatus competitionStatus, RaceSession stage,
            SessionStatus stageStatus) {
        competition.setStatus(competitionStatus);
        competitionRepo.save(competition);
        stage.setStatus(stageStatus);
        raceSessionRepo.save(stage);
        liveBoardEvents.changed(stage.getSessionId(), stageStatus.name());
    }

    // race place for a qualifier: already approved, nothing more to pay
    private RaceBooking newRacePlace(RaceBooking qualifier, RaceSession race, int gridPosition) {
        RaceBooking place = new RaceBooking(qualifier.getUser(), race, qualifier.getCarNumber(),
                qualifier.getCarCategory(), qualifier.getDriverLicenseNumber(), null);
        place.setStatus(BookingStatus.APPROVED);
        place.setTotalPrice(0.0);
        place.setTermsAcceptedAt(qualifier.getTermsAcceptedAt());
        place.setGridPosition(gridPosition);
        return place;
    }

    private RaceBooking findRunningCar(Long raceBookingId) {
        RaceBooking booking = raceBookingRepo.findById(raceBookingId)
                .orElseThrow(() -> new BookingException("Race booking with ID: " + raceBookingId + " not found."));
        RaceSession session = booking.getSession();
        // practice cars have their own control page
        Optional.of(session)
                .filter(stage -> !stage.isPractice())
                .orElseThrow(() -> new BookingException("Car #" + booking.getCarNumber()
                        + " is in a practice session - use Practice Control"));
        // laps and retirements only while the stage is running
        Optional.of(session)
                .filter(stage -> stage.getStatus() == SessionStatus.LIVE)
                .orElseThrow(() -> new BookingException("This part of the competition is not running (it is "
                        + session.getStatus() + ")"));
        // only an approved car
        Optional.of(booking)
                .filter(car -> BookingStatus.APPROVED.equals(car.getStatus()))
                .orElseThrow(() -> new BookingException("Car #" + booking.getCarNumber()
                        + " is not approved for this session"));
        return booking;
    }
}
