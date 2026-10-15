package com.examly.springapp.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.examly.springapp.config.CacheConfig;
import com.examly.springapp.dto.response.ChartPoint;
import com.examly.springapp.dto.response.DriverStatsResponse;
import com.examly.springapp.dto.response.LeaderboardEntry;
import com.examly.springapp.model.CompetitionPhase;
import com.examly.springapp.model.RaceSession;
import com.examly.springapp.model.SessionStatus;
import com.examly.springapp.model.User;
import com.examly.springapp.repository.DriverTotals;
import com.examly.springapp.repository.LeaderboardRow;
import com.examly.springapp.repository.MonthAmount;
import com.examly.springapp.repository.RaceBookingRepo;

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
