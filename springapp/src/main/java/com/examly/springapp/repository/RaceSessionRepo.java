package com.examly.springapp.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.examly.springapp.model.CompetitionPhase;
import com.examly.springapp.model.RaceSession;
import com.examly.springapp.model.SessionStatus;
import com.examly.springapp.model.SessionType;

import jakarta.persistence.LockModeType;

public interface RaceSessionRepo extends JpaRepository<RaceSession, Long> {

    // StatusNot(CANCELLED) = everything still on the calendar
    List<RaceSession> findByTrackTrackIdAndSessionDateAndStatusNotOrderByStartTime(Long trackId,
            LocalDate sessionDate, SessionStatus status);

    List<RaceSession> findAllByOrderBySessionDateDescStartTimeAsc();

    // a track that ever had a session cannot be deleted, only deactivated
    boolean existsByTrackTrackId(Long trackId);

    // deactivate is blocked while a track has SCHEDULED / LIVE sessions
    boolean existsByTrackTrackIdAndStatusIn(Long trackId, List<SessionStatus> statuses);

    Optional<RaceSession> findByCompetitionCompetitionIdAndPhase(Long competitionId, CompetitionPhase phase);

    // select ... for update: approvals and paid bookings of one session go one at a time (last seat, car number)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RaceSession> findWithLockBySessionId(Long sessionId);

    // per track: how many sessions a driver can still book + the cheapest price
    // bookable = SCHEDULED, not a competition race stage, start time not reached, a seat still free
    @Query("select s.track.trackId as trackId, count(s) as sessions, min(s.price) as fromPrice from RaceSession s"
            + " where s.status = ?1 and (s.phase is null or s.phase <> ?2)"
            + " and (s.sessionDate > ?3 or (s.sessionDate = ?3 and s.startTime > ?4))"
            + " and (select count(b) from RaceBooking b where b.session = s and b.status = 'Approved') < s.maxCars"
            + " group by s.track.trackId")
    List<TrackOffer> findBookablePerTrack(SessionStatus scheduled, CompetitionPhase racing, LocalDate today,
            LocalTime now);

    // the chatbot booking tool picks from these
    List<RaceSession> findByStatusAndSessionDateGreaterThanEqualOrderBySessionDateAscStartTimeAsc(SessionStatus status,
            LocalDate from);

    List<RaceSession> findByStatus(SessionStatus status);

    long countByStatus(SessionStatus status);

    // races per month, cancelled ones not counted
    @Query("select month(s.sessionDate) as month, count(s) as amount from RaceSession s"
            + " where year(s.sessionDate) = ?1 and s.status <> ?2 group by month(s.sessionDate)")
    List<MonthAmount> countPerMonth(int year, SessionStatus notThisStatus);

    // admin list page, search: track or competition name
    @EntityGraph(attributePaths = { "track", "competition" })
    @Query("select s from RaceSession s left join s.competition c where (?1 is null or s.status = ?1)"
            + " and (?2 is null or s.sessionType = ?2)"
            + " and (lower(s.track.trackName) like ?3 or lower(coalesce(c.competitionName, '')) like ?3)")
    Page<RaceSession> searchPage(SessionStatus status, SessionType type, String pattern, Pageable pageable);
}
