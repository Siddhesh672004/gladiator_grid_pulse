package com.examly.springapp.repository;

// approved cars of one session, many sessions counted in one query
public interface SessionCars {

    Long getSessionId();

    long getCars();
}
