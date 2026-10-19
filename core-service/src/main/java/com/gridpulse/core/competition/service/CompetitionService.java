package com.gridpulse.core.competition.service;

import java.util.List;

import com.gridpulse.core.competition.data.Competition;
import com.gridpulse.core.competition.data.CompetitionPhase;
import com.gridpulse.core.competition.dto.CompetitionRequest;
import com.gridpulse.core.competition.dto.CompetitionResponse;
import com.gridpulse.core.competition.dto.CompetitionRule;
import com.gridpulse.core.session.data.RaceSession;

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
