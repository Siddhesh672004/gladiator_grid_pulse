package com.examly.springapp.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.examly.springapp.dto.request.SessionRequest;
import com.examly.springapp.dto.response.PageResponse;
import com.examly.springapp.dto.response.SessionResponse;
import com.examly.springapp.model.RaceTrack;

// sessions the admin schedules, drivers and spectators list them
public interface RaceSessionService {

    SessionResponse addPracticeSession(SessionRequest request);

    // only while SCHEDULED
    SessionResponse updatePracticeSession(Long sessionId, SessionRequest request);

    // only while SCHEDULED, its bookings are rejected and the drivers get an email
    SessionResponse cancelSession(Long sessionId);

    List<SessionResponse> getAllSessions();

    // admin list: newest date first, search = track or competition, status / type (Practice, Competition) or All
    PageResponse<SessionResponse> getSessionPage(String search, String status, String type, int page, int size);

    SessionResponse getSessionById(Long sessionId);

    List<SessionResponse> getSessionsForTrackOnDate(Long trackId, LocalDate date);

    // used for practice and competition. ignoreSessionId = the session being edited (or null)
    void checkSlot(RaceTrack track, LocalDate date, LocalTime startTime, LocalTime endTime, Long ignoreSessionId);
}
