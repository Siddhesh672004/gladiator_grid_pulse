package com.gridpulse.ai.copilot.data;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

public interface PendingActionRepo extends JpaRepository<PendingAction, String> {

    // a double click waits for the first click's transaction: one Confirm runs the action
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PendingAction> findWithLockByToken(String token);
}
