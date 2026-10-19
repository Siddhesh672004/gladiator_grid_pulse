package com.gridpulse.core.service;

import java.util.List;

import com.gridpulse.core.dto.CompetitionRule;
import com.gridpulse.core.dto.request.CompetitionRequest;
import com.gridpulse.core.dto.response.CompetitionResponse;
import com.gridpulse.core.model.Competition;
import com.gridpulse.core.model.CompetitionPhase;
import com.gridpulse.core.model.RaceSession;

// create / list / cancel competitions, running one is in CompetitionControlService
public interface CompetitionService {

    CompetitionResponse createCompetition(CompetitionRequest request);

    List<CompetitionResponse> getAllCompetitions();

    CompetitionResponse getCompetitionById(Long competitionId);

    // only while UPCOMING: both sessions are cancelled and their bookings rejected
    CompetitionResponse cancelCompetition(Long competitionId);

    CompetitionRule getRuleForTrack(Long trackId);

    // also used by CompetitionControlServiceImpl, 404 when missing
    Competition findCompetition(Long competitionId);

    RaceSession findStage(Competition competition, CompetitionPhase phase);
}
