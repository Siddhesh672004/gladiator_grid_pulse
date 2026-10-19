package com.gridpulse.core.competition.data;

import java.time.LocalDate;

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

// one competition = a Qualifying session + a Racing session (two RaceSession rows point here)
// the rule numbers are copied from RaceRules.ruleFor when it is created, later rule changes do not touch it
@Entity
@Getter
@Setter
@NoArgsConstructor
public class Competition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long competitionId;

    @Column(length = 100, unique = true)
    private String competitionName;

    @ManyToOne
    @JoinColumn(name = "trackId")
    private RaceTrack track;

    private LocalDate competitionDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private CompetitionStatus status = CompetitionStatus.UPCOMING;

    private Integer qualifyingLaps;
    private Integer raceLaps;
    private Integer maxCars;

    public Competition(String competitionName, RaceTrack track, LocalDate competitionDate, Integer qualifyingLaps,
            Integer raceLaps, Integer maxCars) {
        this.competitionName = competitionName;
        this.track = track;
        this.competitionDate = competitionDate;
        this.qualifyingLaps = qualifyingLaps;
        this.raceLaps = raceLaps;
        this.maxCars = maxCars;
    }
}
