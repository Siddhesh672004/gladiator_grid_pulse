package com.gridpulse.core.competition.data;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CompetitionRepo extends JpaRepository<Competition, Long> {

    List<Competition> findAllByOrderByCompetitionDateDesc();

    boolean existsByCompetitionNameIgnoreCase(String competitionName);
}
