package com.gridpulse.core.mapper;

import org.springframework.stereotype.Component;

import com.gridpulse.core.dto.CompetitionRule;
import com.gridpulse.core.dto.request.CompetitionRequest;
import com.gridpulse.core.dto.response.CompetitionResponse;
import com.gridpulse.core.dto.response.SessionResponse;
import com.gridpulse.core.model.Competition;
import com.gridpulse.core.model.RaceTrack;

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
