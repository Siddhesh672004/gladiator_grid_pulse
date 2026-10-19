package com.gridpulse.core.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.gridpulse.core.config.CacheConfig;
import com.gridpulse.core.dto.response.ChartPoint;
import com.gridpulse.core.dto.response.DriverStatsResponse;
import com.gridpulse.core.dto.response.LeaderboardEntry;
import com.gridpulse.core.model.CompetitionPhase;
import com.gridpulse.core.model.RaceSession;
import com.gridpulse.core.model.SessionStatus;
import com.gridpulse.core.model.User;
import com.gridpulse.core.repository.DriverTotals;
import com.gridpulse.core.repository.LeaderboardRow;
import com.gridpulse.core.repository.MonthAmount;
import com.gridpulse.core.repository.RaceBookingRepo;

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
