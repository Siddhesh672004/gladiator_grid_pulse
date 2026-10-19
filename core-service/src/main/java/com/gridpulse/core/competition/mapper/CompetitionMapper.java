package com.gridpulse.core.competition.mapper;

import org.springframework.stereotype.Component;

import com.gridpulse.core.competition.data.Competition;
import com.gridpulse.core.competition.dto.CompetitionRequest;
import com.gridpulse.core.competition.dto.CompetitionResponse;
import com.gridpulse.core.competition.dto.CompetitionRule;
import com.gridpulse.core.session.dto.SessionResponse;
import com.gridpulse.core.track.data.RaceTrack;

@Component
public class CompetitionMapper {

    // the rule numbers are copied in (a snapshot)
    public Competition toEntity(CompetitionRequest request, RaceTrack track, CompetitionRule rule) {
        return new Competition(request.competitionName().trim(), track, request.competitionDate(),
                rule.qualifyingLaps(), rule.raceLaps(), rule.maxCars());
    }

    public CompetitionResponse toResponse(Competition competition, SessionResponse qualifyingSession,
            SessionResponse raceSession) {
        RaceTrack track = competition.getTrack();
        return new CompetitionResponse(
                competition.getCompetitionId(),
                competition.getCompetitionName(),
                track.getTrackId(),
                track.getTrackName(),
                track.getTrackCategory(),
                competition.getCompetitionDate(),
                competition.getStatus(),
                competition.getQualifyingLaps(),
                competition.getRaceLaps(),
                competition.getMaxCars(),
                qualifyingSession,
                raceSession);
    }
}
