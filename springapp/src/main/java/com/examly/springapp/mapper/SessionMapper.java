package com.examly.springapp.mapper;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.examly.springapp.dto.request.SessionRequest;
import com.examly.springapp.dto.response.SessionResponse;
import com.examly.springapp.model.Competition;
import com.examly.springapp.model.RaceSession;
import com.examly.springapp.model.RaceTrack;

@Component
public class SessionMapper {

    public RaceSession toEntity(SessionRequest request, RaceTrack track) {
        return new RaceSession(track, request.sessionDate(), request.startTime(), request.endTime(),
                request.price(), request.maxCars());
    }

    public void updateEntity(RaceSession session, SessionRequest request, RaceTrack track) {
        session.setTrack(track);
        session.setSessionDate(request.sessionDate());
        session.setStartTime(request.startTime());
        session.setEndTime(request.endTime());
        session.setPrice(request.price());
        session.setMaxCars(request.maxCars());
    }

    public SessionResponse toResponse(RaceSession session) {
        return toResponse(session, null);
    }

    public SessionResponse toResponse(RaceSession session, Long approvedCars) {
        RaceTrack track = session.getTrack();
        // a practice session has no competition
        Optional<Competition> competition = Optional.ofNullable(session.getCompetition());
        return new SessionResponse(
                session.getSessionId(),
                track.getTrackId(),
                track.getTrackName(),
                track.getLocation(),
                session.getSessionType(),
                session.getPhase(),
                competition.map(Competition::getCompetitionId).orElse(null),
                competition.map(Competition::getCompetitionName).orElse(null),
                session.getSessionDate(),
                session.getStartTime(),
                session.getEndTime(),
                session.getPrice(),
                session.getMaxCars(),
                session.getStatus(),
                approvedCars);
    }
}
