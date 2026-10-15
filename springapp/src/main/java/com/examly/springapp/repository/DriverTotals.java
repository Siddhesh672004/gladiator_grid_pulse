package com.examly.springapp.repository;

// one driver's numbers over all completed sessions, counted by the database in one query
public interface DriverTotals {

    long getSessions();

    long getLaps();

    long getCompetitions();

    long getWins();

    long getPodiums();

    long getDnfs();

    Integer getBestRace();

    Integer getBestQualifying();
}
