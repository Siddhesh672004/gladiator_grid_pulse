package com.gridpulse.core.stats.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;

import com.gridpulse.common.paging.ChartPoint;
import com.gridpulse.common.paging.MonthAmount;
import com.gridpulse.core.account.data.Role;
import com.gridpulse.core.account.data.User;
import com.gridpulse.core.account.data.UserRepo;
import com.gridpulse.core.booking.data.BookingStatus;
import com.gridpulse.core.booking.data.RaceBooking;
import com.gridpulse.core.booking.data.RaceBookingRepo;
import com.gridpulse.core.feedback.data.FeedbackRepo;
import com.gridpulse.core.racecontrol.service.RaceRules;
import com.gridpulse.core.session.data.RaceSession;
import com.gridpulse.core.session.data.RaceSessionRepo;
import com.gridpulse.core.session.data.SessionStatus;
import com.gridpulse.core.stats.dto.AdminDashboardResponse;
import com.gridpulse.core.stats.dto.BookingRaceStatistic;
import com.gridpulse.core.stats.dto.DashboardMetrics;
import com.gridpulse.core.stats.dto.LiveRaceResponse;
import com.gridpulse.core.stats.dto.RecentRegistrationResponse;
import com.gridpulse.core.stats.dto.RevenueResponse;
import com.gridpulse.core.track.data.RaceTrackRepo;

import lombok.RequiredArgsConstructor;

// every number is counted by the database, no table of its own
// the money numbers come from payment-service (one snapshot there), 0 when it does not answer
@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final UserRepo userRepo;
    private final RaceTrackRepo raceTrackRepo;
    private final RaceBookingRepo raceBookingRepo;
    private final RaceSessionRepo raceSessionRepo;
    private final FeedbackRepo feedbackRepo;
    private final PaymentClient paymentClient;

    @Override
    public AdminDashboardResponse getDashboard() {
        RevenueResponse statement = paymentClient.summary();
        return new AdminDashboardResponse(metrics(statement), registrationStatus(), trackCategories(),
                bookingRaceStatistics(), recentRegistrations(), getLiveRaces(), statement);
    }

    @Override
    public List<LiveRaceResponse> getLiveRaces() {
        return raceSessionRepo.findByStatus(SessionStatus.LIVE).stream()
                .map(this::liveRace)
                .toList();
    }

    private DashboardMetrics metrics(RevenueResponse statement) {
        return new DashboardMetrics(
                userRepo.count(),
                userRepo.countByUserRole(Role.USER),
                raceTrackRepo.count(),
                raceBookingRepo.count(),
                raceSessionRepo.countByStatus(SessionStatus.LIVE),
                statement.collected(),
                feedbackRepo.count(),
                feedbackRepo.countByUrgentTrue());
    }

    // Pending = switched on but date of birth / license still missing
    private List<ChartPoint> registrationStatus() {
        long active = userRepo.countByUserRoleAndEnabledTrueAndDateOfBirthIsNotNullAndDriverLicenseNumberIsNotNull(Role.USER);
        long switchedOn = userRepo.countByUserRoleAndEnabled(Role.USER, true);
        long inactive = userRepo.countByUserRoleAndEnabled(Role.USER, false);
        return List.of(new ChartPoint("Active", active), new ChartPoint("Pending", switchedOn - active),
                new ChartPoint("Inactive", inactive));
    }

    private List<ChartPoint> trackCategories() {
        return raceTrackRepo.countPerCategory().stream()
                .map(row -> new ChartPoint(row.getLabel(), row.getAmount().doubleValue()))
                .toList();
    }

    private List<BookingRaceStatistic> bookingRaceStatistics() {
        int year = LocalDate.now().getYear();
        double[] bookings = MonthAmount.twelveMonths(raceBookingRepo.countPerMonth(year));
        double[] races = MonthAmount.twelveMonths(raceSessionRepo.countPerMonth(year, SessionStatus.CANCELLED));
        // one row per month, Jan to Dec
        return IntStream.range(0, 12)
                .mapToObj(month -> new BookingRaceStatistic(MonthAmount.MONTHS[month], (long) bookings[month],
                        (long) races[month]))
                .toList();
    }

    private List<RecentRegistrationResponse> recentRegistrations() {
        return raceBookingRepo.findTop8ByOrderByRaceBookingIdDesc().stream()
                .map(this::recentRegistration)
                .toList();
    }

    // active cars = still RACING, current lap = the leader, progress only for a competition stage (it has a lap limit)
    private LiveRaceResponse liveRace(RaceSession session) {
        List<RaceBooking> cars = raceBookingRepo.findBySessionSessionIdAndStatus(session.getSessionId(),
                BookingStatus.APPROVED);
        // cars still out on the track
        int activeCars = (int) cars.stream()
                .filter(car -> RaceRules.RACING.equals(RaceRules.stateOf(car)))
                .count();
        // the leader's lap count is the lap of the race, 0 before anyone has a lap
        int currentLap = cars.stream()
                .mapToInt(RaceRules::lapsOf)
                .reduce(0, Math::max);
        Integer totalLaps = session.lapLimit();
        // practice has no lap limit, so no progress either
        Integer progress = Optional.ofNullable(totalLaps)
                .map(laps -> Math.min(100, currentLap * 100 / laps))
                .orElse(null);
        return new LiveRaceResponse(session.getSessionId(), session.getTrack().getTrackName(), kind(session),
                session.getStartTime(), "RUNNING", activeCars, currentLap, totalLaps, progress);
    }

    private RecentRegistrationResponse recentRegistration(RaceBooking booking) {
        RaceSession session = booking.getSession();
        User driver = booking.getUser();
        return new RecentRegistrationResponse(
                booking.getRaceBookingId(),
                driver.getUsername(),
                driver.getEmail(),
                booking.getTermsAcceptedAt(),
                booking.getStatus(),
                driver.getUserRole().getLabel(),
                session.getTrack().getTrackName(),
                kind(session) + " · " + session.getSessionDate() + " " + session.getStartTime(),
                RaceRules.stateOf(booking));
    }

    private String kind(RaceSession session) {
        // practice, else the stage of the competition
        return session.isPractice() ? "Practice" : session.isQualifying() ? "Qualifying" : "Race";
    }
}
