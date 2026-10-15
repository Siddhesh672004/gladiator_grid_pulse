package com.examly.springapp.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.examly.springapp.model.RaceBooking;

public interface RaceBookingRepo extends JpaRepository<RaceBooking, Long> {

    List<RaceBooking> findTop8ByOrderByRaceBookingIdDesc();

    @Query("select month(b.session.sessionDate) as month, count(b) as amount from RaceBooking b"
            + " where year(b.session.sessionDate) = ?1 group by month(b.session.sessionDate)")
    List<MonthAmount> countPerMonth(int year);

    List<RaceBooking> findByUserUserId(int userId);

    List<RaceBooking> findBySessionSessionIdAndStatus(Long sessionId, String status);

    // seats taken = Approved bookings
    long countBySessionSessionIdAndStatus(Long sessionId, String status);

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

    // admin list page: rows come with driver, session, track and payment in the same query (no select per row)
    @EntityGraph(attributePaths = { "user", "session", "session.track", "session.competition", "payment" })
    @Query("select b from RaceBooking b where (?1 is null or b.status = ?1)"
            + " and lower(b.session.track.trackName) like ?2")
    Page<RaceBooking> searchPage(String status, String pattern, Pageable pageable);

    // approved cars of every session on a page in one query
    @Query("select b.session.sessionId as sessionId, count(b) as cars from RaceBooking b"
            + " where b.session.sessionId in ?1 and b.status = 'Approved' group by b.session.sessionId")
    List<SessionCars> countApprovedPerSession(Collection<Long> sessionIds);
}
