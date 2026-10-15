package com.gridpulse.ai.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gridpulse.ai.entity.ActionLog;

public interface ActionLogRepo extends JpaRepository<ActionLog, Long> {
}
