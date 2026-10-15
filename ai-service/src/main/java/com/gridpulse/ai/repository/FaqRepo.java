package com.gridpulse.ai.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gridpulse.ai.entity.FaqEntity;

public interface FaqRepo extends JpaRepository<FaqEntity, Long> {

}
