package com.examly.springapp.service;

import java.util.List;

import com.examly.springapp.dto.CompetitionRule;
import com.examly.springapp.dto.request.CompetitionRequest;
import com.examly.springapp.dto.response.CompetitionResponse;
import com.examly.springapp.model.Competition;
import com.examly.springapp.model.CompetitionPhase;
import com.examly.springapp.model.RaceSession;

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
