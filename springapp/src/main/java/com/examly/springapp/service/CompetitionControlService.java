package com.examly.springapp.service;

import com.examly.springapp.dto.response.CompetitionResponse;
import com.examly.springapp.dto.response.LiveStandingResponse;

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
