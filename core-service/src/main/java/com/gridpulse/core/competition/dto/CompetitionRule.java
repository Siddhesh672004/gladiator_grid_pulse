package com.gridpulse.core.competition.dto;

// fixed rule of one track category, copied into a Competition when it is created
public record CompetitionRule(String trackCategory, int qualifyingLaps, int raceLaps, int maxCars) {
}
