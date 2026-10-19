package com.gridpulse.core.competition.service;

import com.gridpulse.core.competition.dto.CompetitionResponse;
import com.gridpulse.core.racecontrol.dto.LiveStandingResponse;

// one method per button: UPCOMING -> QUALIFYING -> QUALIFYING_COMPLETED -> RACING -> COMPLETED
// a button at the wrong moment is a BookingException (400)
public interface CompetitionControlService {

    CompetitionResponse startQualifying(Long competitionId);

    // the qualifying result becomes the starting grid
    CompetitionResponse finishQualifying(Long competitionId);

    CompetitionResponse startRace(Long competitionId);

    CompetitionResponse finishRace(Long competitionId);

    // at the lap limit the car is FINISHED on its own
    LiveStandingResponse addLap(Long raceBookingId);

    LiveStandingResponse retireCar(Long raceBookingId);
}
