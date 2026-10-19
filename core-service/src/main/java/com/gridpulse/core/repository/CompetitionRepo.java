package com.gridpulse.core.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gridpulse.core.model.Competition;

public interface CompetitionRepo extends JpaRepository<Competition, Long> {

    List<Competition> findAllByOrderByCompetitionDateDesc();

    boolean existsByCompetitionNameIgnoreCase(String competitionName);
}
