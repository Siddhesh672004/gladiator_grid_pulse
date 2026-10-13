package com.examly.springapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.examly.springapp.entity.FaqEntity;

public interface FaqRepo extends JpaRepository<FaqEntity, Long> {

}
