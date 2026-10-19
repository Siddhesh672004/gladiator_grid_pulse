package com.gridpulse.core.stats.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.gridpulse.common.paging.ChartPoint;
import com.gridpulse.common.paging.MonthAmount;
import com.gridpulse.core.account.data.User;
import com.gridpulse.core.account.service.UserService;
import com.gridpulse.core.booking.data.RaceBookingRepo;
import com.gridpulse.core.competition.data.CompetitionPhase;
import com.gridpulse.core.platform.config.CacheConfig;
import com.gridpulse.core.session.data.RaceSession;
import com.gridpulse.core.session.data.SessionStatus;
import com.gridpulse.core.stats.data.DriverTotals;
import com.gridpulse.core.stats.data.LeaderboardRow;
import com.gridpulse.core.stats.dto.DriverStatsResponse;
import com.gridpulse.core.stats.dto.LeaderboardEntry;

import lombok.RequiredArgsConstructor;

// the database adds the numbers up (grouped queries), no booking list is loaded here
@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private static final int LEADERBOARD_MAX = 20;

    private final RaceBookingRepo raceBookingRepo;
    private final UserService userService;

    @Override
    public DriverStatsResponse getMyStats() {
        User driver = userService.getLoggedInUser();
        DriverTotals totals = raceBookingRepo.driverTotals(driver.getUserId(), CompetitionPhase.QUALIFYING,
                CompetitionPhase.RACING, SessionStatus.COMPLETED);
        // laps per month of this year (india date), jan to dec
        double[] months = MonthAmount.twelveMonths(raceBookingRepo.lapsPerMonth(driver.getUserId(),
                SessionStatus.COMPLETED, RaceSession.today().getYear()));
        List<ChartPoint> lapsPerMonth = IntStream.range(0, 12)
                .mapToObj(month -> new ChartPoint(MonthAmount.MONTHS[month], months[month]))
                .toList();
        return new DriverStatsResponse(totals.getSessions(), totals.getLaps(), totals.getCompetitions(),
                totals.getWins(), totals.getPodiums(), totals.getDnfs(), totals.getBestRace(),
                totals.getBestQualifying(), lapsPerMonth);
    }

    // redis keeps each list 5 minutes, a finished session clears it (RaceControl / CompetitionControl)
    @Override
    @Cacheable(cacheNames = CacheConfig.LEADERBOARD, key = "#limit")
    public List<LeaderboardEntry> getLeaderboard(int limit) {
        Optional.of(limit)
                .filter(rows -> rows >= 1 && rows <= LEADERBOARD_MAX)
                .orElseThrow(() -> new IllegalArgumentException("Limit must be between 1 and " + LEADERBOARD_MAX));
        List<LeaderboardRow> rows = raceBookingRepo.leaderboard(CompetitionPhase.RACING, SessionStatus.COMPLETED,
                PageRequest.of(0, limit));
        // position 1, 2 .. in the order of the query
        return IntStream.range(0, rows.size())
                .mapToObj(i -> new LeaderboardEntry(i + 1, rows.get(i).getUsername(), rows.get(i).getWins(),
                        rows.get(i).getPodiums(), rows.get(i).getLaps()))
                .toList();
    }
}
