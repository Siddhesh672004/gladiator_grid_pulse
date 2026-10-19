package com.gridpulse.core.model;

// one admin button per step: UPCOMING -> QUALIFYING -> QUALIFYING_COMPLETED -> RACING -> COMPLETED
// an UPCOMING competition can also be CANCELLED
public enum CompetitionStatus {
    UPCOMING,
    QUALIFYING,
    QUALIFYING_COMPLETED,
    RACING,
    COMPLETED,
    CANCELLED
}
