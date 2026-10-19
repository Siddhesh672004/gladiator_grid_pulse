package com.gridpulse.core.session.data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;

import com.gridpulse.core.competition.data.Competition;
import com.gridpulse.core.competition.data.CompetitionPhase;
import com.gridpulse.core.track.data.RaceTrack;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// WHEN something happens on a track. the admin creates it, drivers pick one
// a practice session stands alone, a competition has two: phase QUALIFYING and RACING
@Entity
@Getter
@Setter
@NoArgsConstructor
public class RaceSession {

    // session times are India time, so a server running in UTC still knows when a session starts
    private static final ZoneId TIME_ZONE = ZoneId.of("Asia/Kolkata");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long sessionId;

    @ManyToOne
    @JoinColumn(name = "trackId")
    private RaceTrack track;

    @ManyToOne
    @JoinColumn(name = "competitionId")
    private Competition competition;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private SessionType sessionType = SessionType.PRACTICE;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CompetitionPhase phase;

    private LocalDate sessionDate;
    private LocalTime startTime;
    private LocalTime endTime;

    // a competition's entry fee sits on its qualifying session, the race is 0
    private Double price;

    private Integer maxCars;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private SessionStatus status = SessionStatus.SCHEDULED;

    public RaceSession(RaceTrack track, LocalDate sessionDate, LocalTime startTime, LocalTime endTime, Double price,
            Integer maxCars) {
        this.track = track;
        this.sessionDate = sessionDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.price = price;
        this.maxCars = maxCars;
    }

    public boolean isPractice() {
        return sessionType == SessionType.PRACTICE;
    }

    public boolean isQualifying() {
        return phase == CompetitionPhase.QUALIFYING;
    }

    public boolean isRacing() {
        return phase == CompetitionPhase.RACING;
    }

    public boolean isScheduled() {
        return status == SessionStatus.SCHEDULED;
    }

    // booking closes at the start time (even if the admin never pressed Start)
    public boolean hasStarted() {
        return !LocalDateTime.of(sessionDate, startTime).isAfter(now());
    }

    // waiting to start, not the race of a competition (its places come from qualifying). a free seat is checked apart
    public boolean isBookable() {
        return isScheduled() && !isRacing() && !hasStarted();
    }

    // laps of a competition stage, null for practice (no lap limit)
    public Integer lapLimit() {
        return Optional.ofNullable(competition)
                .filter(stage -> !isPractice())
                .map(stage -> isQualifying() ? stage.getQualifyingLaps() : stage.getRaceLaps())
                .orElse(null);
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(TIME_ZONE);
    }

    public static LocalDate today() {
        return now().toLocalDate();
    }
}
