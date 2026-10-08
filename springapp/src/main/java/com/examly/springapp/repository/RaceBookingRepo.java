package com.examly.springapp.repository;

import java.time.LocalDate;
import java.util.List;

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
}
