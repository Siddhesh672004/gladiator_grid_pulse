package com.examly.springapp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.examly.springapp.model.Competition;

public interface CompetitionRepo extends JpaRepository<Competition, Long> {

    List<Competition> findAllByOrderByCompetitionDateDesc();

    boolean existsByCompetitionNameIgnoreCase(String competitionName);
}
