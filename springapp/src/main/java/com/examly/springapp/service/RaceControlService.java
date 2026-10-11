package com.examly.springapp.service;

import java.util.List;

import com.examly.springapp.dto.response.LiveStandingResponse;

// race control for practice sessions (a competition is run from CompetitionControlService)
public interface RaceControlService {

    List<LiveStandingResponse> getLiveStandings(Long sessionId);

    LiveStandingResponse startRace(Long raceBookingId);

    LiveStandingResponse incrementLap(Long raceBookingId);

    LiveStandingResponse retireCar(Long raceBookingId);

    // not possible once the session is finished
    List<LiveStandingResponse> resetSession(Long sessionId);

    List<LiveStandingResponse> finishSession(Long sessionId);
}
