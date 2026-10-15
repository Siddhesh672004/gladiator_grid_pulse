package com.examly.springapp.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.examly.springapp.model.CompetitionPhase;
import com.examly.springapp.model.RaceBooking;
import com.examly.springapp.model.SessionStatus;

public interface RaceBookingRepo extends JpaRepository<RaceBooking, Long> {

    List<RaceBooking> findTop8ByOrderByRaceBookingIdDesc();

    @Query("select month(b.session.sessionDate) as month, count(b) as amount from RaceBooking b"
            + " where year(b.session.sessionDate) = ?1 group by month(b.session.sessionDate)")
    List<MonthAmount> countPerMonth(int year);

    List<RaceBooking> findByUserUserId(int userId);

    List<RaceBooking> findBySessionSessionIdAndStatus(Long sessionId, String status);

    // seats taken = Approved bookings
    long countBySessionSessionIdAndStatus(Long sessionId, String status);

    // the queue of a session: oldest first
    List<RaceBooking> findBySessionSessionIdAndStatusOrderByRaceBookingIdAsc(Long sessionId, String status);

    // how many of the queue are before this booking
    long countBySessionSessionIdAndStatusAndRaceBookingIdLessThan(Long sessionId, String status, Long raceBookingId);

    // rejected bookings free their car number
    boolean existsBySessionSessionIdAndCarNumberAndStatusNot(Long sessionId, Integer carNumber, String status);

    // same, ignoring one booking (a rejected booking being approved again)
    boolean existsBySessionSessionIdAndCarNumberAndStatusNotAndRaceBookingIdNot(Long sessionId, Integer carNumber,
            String status, Long raceBookingId);

    // did this driver take part? (feedback is only allowed then)
    boolean existsBySessionSessionIdAndUserUserIdAndStatus(Long sessionId, int userId, String status);

    // what one driver booked on one day on any track (stops two bookings at the same time)
    List<RaceBooking> findByUserUserIdAndSessionSessionDateAndStatusIn(int userId, LocalDate sessionDate,
            List<String> statuses);

    // admin list page: rows come with driver, session and track in the same query (no select per row)
    @EntityGraph(attributePaths = { "user", "session", "session.track", "session.competition" })
    @Query("select b from RaceBooking b where (?1 is null or b.status = ?1)"
            + " and lower(b.session.track.trackName) like ?2")
    Page<RaceBooking> searchPage(String status, String pattern, Pageable pageable);

    // driver stats: the approved cars of completed sessions, ?2 = QUALIFYING, ?3 = RACING, ?4 = COMPLETED
    @Query("select count(b) as sessions, coalesce(sum(b.lapCount), 0) as laps,"
            + " coalesce(sum(case when s.phase = ?2 then 1 else 0 end), 0) as competitions,"
            + " coalesce(sum(case when s.phase = ?3 and b.finalPosition = 1 then 1 else 0 end), 0) as wins,"
            + " coalesce(sum(case when s.phase = ?3 and b.finalPosition <= 3 then 1 else 0 end), 0) as podiums,"
            + " coalesce(sum(case when b.raceState = 'RETIRED' then 1 else 0 end), 0) as dnfs,"
            + " min(case when s.phase = ?3 then b.finalPosition end) as bestRace,"
            + " min(case when s.phase = ?2 then b.finalPosition end) as bestQualifying"
            + " from RaceBooking b join b.session s"
            + " where b.user.userId = ?1 and b.status = 'Approved' and s.status = ?4")
    DriverTotals driverTotals(int userId, CompetitionPhase qualifying, CompetitionPhase racing, SessionStatus completed);

    @Query("select month(s.sessionDate) as month, coalesce(sum(b.lapCount), 0) as amount"
            + " from RaceBooking b join b.session s"
            + " where b.user.userId = ?1 and b.status = 'Approved' and s.status = ?2 and year(s.sessionDate) = ?3"
            + " group by month(s.sessionDate)")
    List<MonthAmount> lapsPerMonth(int userId, SessionStatus completed, int year);

    // wins first, then podiums, then laps. ?1 = RACING, ?2 = COMPLETED, the page size is the limit
    @Query("select b.user.username as username,"
            + " coalesce(sum(case when s.phase = ?1 and b.finalPosition = 1 then 1 else 0 end), 0) as wins,"
            + " coalesce(sum(case when s.phase = ?1 and b.finalPosition <= 3 then 1 else 0 end), 0) as podiums,"
            + " coalesce(sum(b.lapCount), 0) as laps"
            + " from RaceBooking b join b.session s"
            + " where b.status = 'Approved' and s.status = ?2"
            + " group by b.user.userId, b.user.username"
            + " order by wins desc, podiums desc, laps desc, b.user.username")
    List<LeaderboardRow> leaderboard(CompetitionPhase racing, SessionStatus completed, Pageable limit);

    Optional<RaceBooking> findByPaymentOrderId(String paymentOrderId);

    // approved cars of every session on a page in one query
    @Query("select b.session.sessionId as sessionId, count(b) as cars from RaceBooking b"
            + " where b.session.sessionId in ?1 and b.status = 'Approved' group by b.session.sessionId")
    List<SessionCars> countApprovedPerSession(Collection<Long> sessionIds);
}
