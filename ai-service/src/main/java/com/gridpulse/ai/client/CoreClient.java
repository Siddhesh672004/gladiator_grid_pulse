package com.gridpulse.ai.client;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PatchExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;

import com.fasterxml.jackson.databind.JsonNode;

// the core's api, called with the caller's own token (config/ClientConfig): the core checks the user itself
// the answers stay json, the tools pick what the model needs. a 4xx -> ToolCallException
@HttpExchange
public interface CoreClient {

    // admin token + internal key
    @GetExchange("/internal/feedback/recent")
    List<RecentFeedback> recentFeedback(@RequestParam int limit);

    // driver token + internal key: every booking rule, the price, waitlisted or not (nothing is saved)
    @PostExchange("/internal/bookings/check")
    JsonNode checkBooking(@RequestBody Map<String, Object> booking);

    @GetExchange("/api/race-track")
    JsonNode tracks();

    @GetExchange("/api/race-track/{trackId}")
    JsonNode track(@PathVariable Long trackId);

    @PostExchange("/api/race-track")
    JsonNode createTrack(@RequestBody Map<String, Object> track);

    @PutExchange("/api/race-track/{trackId}")
    JsonNode updateTrack(@PathVariable Long trackId, @RequestBody Map<String, Object> track);

    @PostExchange("/api/race-track/{trackId}/activate")
    JsonNode activateTrack(@PathVariable Long trackId);

    @PostExchange("/api/race-track/{trackId}/deactivate")
    JsonNode deactivateTrack(@PathVariable Long trackId);

    @DeleteExchange("/api/race-track/{trackId}")
    JsonNode deleteTrack(@PathVariable Long trackId);

    @GetExchange("/api/sessions/track/{trackId}")
    JsonNode sessionsOnDate(@PathVariable Long trackId, @RequestParam LocalDate date);

    @GetExchange("/api/sessions/{sessionId}")
    JsonNode session(@PathVariable Long sessionId);

    @GetExchange("/api/sessions/page")
    JsonNode sessionPage(@RequestParam(required = false) String search, @RequestParam(required = false) String status,
            @RequestParam int page, @RequestParam int size);

    @PostExchange("/api/sessions")
    JsonNode createSession(@RequestBody Map<String, Object> session);

    @PutExchange("/api/sessions/{sessionId}")
    JsonNode updateSession(@PathVariable Long sessionId, @RequestBody Map<String, Object> session);

    @PostExchange("/api/sessions/{sessionId}/cancel")
    JsonNode cancelSession(@PathVariable Long sessionId);

    @GetExchange("/api/competitions")
    JsonNode competitions();

    @GetExchange("/api/competitions/{competitionId}")
    JsonNode competition(@PathVariable Long competitionId);

    @PostExchange("/api/competitions")
    JsonNode createCompetition(@RequestBody Map<String, Object> competition);

    @PostExchange("/api/competitions/{competitionId}/cancel")
    JsonNode cancelCompetition(@PathVariable Long competitionId);

    @GetExchange("/api/race-booking/user/{userId}")
    JsonNode bookingsOfUser(@PathVariable int userId);

    @GetExchange("/api/race-booking/{bookingId}")
    JsonNode booking(@PathVariable Long bookingId);

    @GetExchange("/api/race-booking/page")
    JsonNode bookingPage(@RequestParam(required = false) String status, @RequestParam int page,
            @RequestParam int size);

    @PutExchange("/api/race-booking/{bookingId}")
    JsonNode decideBooking(@PathVariable Long bookingId, @RequestBody Map<String, Object> decision);

    @DeleteExchange("/api/race-booking/{bookingId}")
    void cancelBooking(@PathVariable Long bookingId);

    @GetExchange("/api/race-control/session/{sessionId}")
    JsonNode standings(@PathVariable Long sessionId);

    @GetExchange("/api/feedback/user/{userId}")
    JsonNode feedbackOfUser(@PathVariable int userId);

    @GetExchange("/api/feedback/{feedbackId}")
    JsonNode feedback(@PathVariable Long feedbackId);

    @GetExchange("/api/feedback/page")
    JsonNode feedbackPage(@RequestParam(required = false) String sentiment, @RequestParam(required = false) String rating,
            @RequestParam boolean urgent, @RequestParam int page, @RequestParam int size);

    @PostExchange("/api/feedback")
    JsonNode createFeedback(@RequestBody Map<String, Object> feedback);

    @DeleteExchange("/api/feedback/{feedbackId}")
    JsonNode deleteFeedback(@PathVariable Long feedbackId);

    @GetExchange("/api/stats/me")
    JsonNode myStats();

    @GetExchange("/api/stats/leaderboard")
    JsonNode leaderboard();

    @GetExchange("/api/profile")
    JsonNode profile();

    @GetExchange("/api/admin/dashboard")
    JsonNode dashboard();

    @GetExchange("/api/admin/users")
    JsonNode drivers(@RequestParam(required = false) String search, @RequestParam(required = false) String status,
            @RequestParam int page, @RequestParam int size);

    @PatchExchange("/api/admin/users/{userId}/status")
    JsonNode setDriverStatus(@PathVariable int userId, @RequestBody Map<String, Object> status);

    @GetExchange("/api/tickets/verify/{code}")
    JsonNode checkTicket(@PathVariable String code);

    @GetExchange("/api/super-admin/admins")
    JsonNode admins();

    @PostExchange("/api/super-admin/admins")
    JsonNode addAdmin(@RequestBody Map<String, Object> admin);

    @PatchExchange("/api/super-admin/admins/{userId}/status")
    JsonNode setAdminStatus(@PathVariable int userId, @RequestBody Map<String, Object> status);
}
