package com.gridpulse.ai.tools;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.StreamSupport;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.gridpulse.ai.client.CoreClient;
import com.gridpulse.ai.client.PaymentClient;
import com.gridpulse.ai.dto.response.ActionResult;
import com.gridpulse.ai.exceptions.ToolCallException;
import com.gridpulse.ai.security.TokenUser;
import com.gridpulse.ai.security.annotation.UserOnly;
import com.gridpulse.ai.service.PendingActions;

import lombok.RequiredArgsConstructor;

// the driver's Copilot: reads about tracks, sessions and their own bookings; book / cancel / feedback only as
// Confirm cards. every call carries the driver's token, the core checks it is their data
@Component
@RequiredArgsConstructor
public class DriverTools implements RoleTools, ConfirmableTools {

    private static final String TERMS = "By confirming you accept the booking terms: a rejected booking, or one you "
            + "cancel while Pending or waitlisted, is refunded in full; an approved booking you cancel is not.";

    private record BookArgs(Long sessionId, String carCategory, Integer carNumber, String comments) {
    }

    private record IdArgs(Long id) {
    }

    private record FeedbackArgs(Long sessionId, Integer rating, String text) {
    }

    private final CoreClient core;
    private final PaymentClient payments;
    private final PendingActions pendingActions;

    @Override
    public Set<String> roles() {
        return Set.of("User");
    }

    @UserOnly
    @Tool(description = "List the race tracks drivers can book, with location, category and the lowest price.")
    public JsonNode listTracks() {
        return Data.pickAll(core.tracks(), "trackId", "trackName", "location", "trackCategory", "isIndoor", "fromPrice",
                "upcomingSessions");
    }

    @UserOnly
    @Tool(description = "Find the sessions open for booking on one date, on every track or on one track.")
    public JsonNode findOpenSessions(
            @ToolParam(description = "date, yyyy-MM-dd") String date,
            @ToolParam(description = "part of the track name, only when the driver named a track", required = false)
            String trackName) {
        LocalDate day = parseDate(date);
        ArrayNode sessions = JsonNodeFactory.instance.arrayNode();
        StreamSupport.stream(core.tracks().spliterator(), false)
                .filter(track -> trackName == null || trackName.isBlank()
                        || Data.text(track, "trackName").toLowerCase().contains(trackName.trim().toLowerCase()))
                .limit(10)
                .flatMap(track -> StreamSupport.stream(core.sessionsOnDate(track.path("trackId").asLong(), day)
                        .spliterator(), false))
                .filter(session -> "SCHEDULED".equals(Data.text(session, "status")))
                .map(session -> Data.pick(session, "sessionId", "trackName", "sessionType", "phase", "competitionName",
                        "sessionDate", "startTime", "endTime", "price", "maxCars", "approvedCars"))
                .forEach(sessions::add);
        return sessions;
    }

    @UserOnly
    @Tool(description = "The driver's own bookings with status, payment status and waitlist place.")
    public JsonNode myBookings() {
        return Data.pickAll(core.bookingsOfUser(TokenUser.current().userId()), "raceBookingId", "session.sessionId",
                "session.trackName", "session.sessionDate", "session.startTime", "session.endTime", "status",
                "carNumber", "carCategory", "totalPrice", "paymentStatus", "waitlistPosition", "adminNote",
                "finalPosition");
    }

    @UserOnly
    @Tool(description = "The feedback the driver has given.")
    public JsonNode myFeedback() {
        return Data.pickAll(core.feedbackOfUser(TokenUser.current().userId()), "feedbackId", "session.sessionId",
                "session.trackName", "date", "rating", "feedbackText");
    }

    @UserOnly
    @Tool(description = "The live standings (laps, positions) of one session.")
    public JsonNode liveStandings(@ToolParam(description = "the session id") Long sessionId) {
        return Data.pickAll(core.standings(sessionId), "user.username", "carNumber", "lapCount", "raceState",
                "finalPosition", "gridPosition");
    }

    @UserOnly
    @Tool(description = "The driver's own racing statistics (sessions, laps, wins, podiums).")
    public JsonNode myStats() {
        return Data.pick(core.myStats(), "sessionsDriven", "totalLaps", "competitionsEntered", "wins", "podiums",
                "dnfs", "bestRaceFinish", "bestQualifying");
    }

    @UserOnly
    @Tool(description = "The driver's profile (name, email, mobile, license, whether it is complete).")
    public JsonNode myProfile() {
        return Data.pick(core.profile(), "username", "email", "mobileNumber", "dateOfBirth", "driverLicenseNumber",
                "profileComplete");
    }

    @UserOnly
    @Tool(description = "The public leaderboard of drivers (wins, podiums, laps).")
    public JsonNode leaderboard() {
        return core.leaderboard();
    }

    @UserOnly
    @Tool(description = "Prepare booking a session for the driver (runs every booking rule first). Nothing is booked "
            + "until the driver presses Confirm; then the payment window opens.")
    public String prepareBook(
            @ToolParam(description = "the session id") Long sessionId,
            @ToolParam(description = "GT3, Formula, Karting, Rally or Stock") String carCategory,
            @ToolParam(description = "car number from 1 to 999") Integer carNumber,
            @ToolParam(description = "a comment for race control", required = false) String comments) {
        BookArgs args = new BookArgs(sessionId, carCategory, carNumber, comments);
        JsonNode quote = core.checkBooking(bookingRequest(args));
        String waitlist = quote.path("waitlisted").asBoolean()
                ? " The session is full: the booking joins the waitlist (refunded if no seat opens)."
                : "";
        String summary = "Book " + Data.text(quote, "description") + " for car #" + carNumber + " (" + carCategory
                + "), pay " + Data.rupees(quote.path("price").asDouble()) + " in the payment window." + waitlist + " "
                + TERMS;
        return pendingActions.prepare("book", args, summary);
    }

    @UserOnly
    @Tool(description = "Prepare cancelling one of the driver's bookings. Nothing changes until the driver presses "
            + "Confirm.")
    public String prepareCancelBooking(@ToolParam(description = "the booking id") Long bookingId) {
        JsonNode booking = core.booking(bookingId);
        String status = Data.text(booking, "status");
        String refund = "PAID".equals(Data.text(booking, "paymentStatus"))
                ? ("Approved".equals(status) ? " It is approved, so the amount is not refunded."
                        : " The " + Data.rupees(booking.path("totalPrice").asDouble()) + " paid is refunded in full.")
                : "";
        String summary = "Cancel booking #" + bookingId + " (car #" + booking.path("carNumber").asInt() + ", "
                + Data.sessionText(booking.path("session")) + ", " + status + ")." + refund;
        return pendingActions.prepare("cancelBooking", new IdArgs(bookingId), summary);
    }

    @UserOnly
    @Tool(description = "Prepare feedback about a completed session the driver took part in. Nothing is saved until "
            + "the driver presses Confirm.")
    public String prepareSubmitFeedback(
            @ToolParam(description = "the session id") Long sessionId,
            @ToolParam(description = "stars from 1 to 5") Integer rating,
            @ToolParam(description = "the feedback text, 10 to 1000 characters") String text) {
        JsonNode session = core.session(sessionId);
        String summary = "Give " + rating + " star(s) for " + Data.sessionText(session) + ": \"" + excerpt(text) + "\"";
        return pendingActions.prepare("submitFeedback", new FeedbackArgs(sessionId, rating, text), summary);
    }

    @UserOnly
    @Tool(description = "Prepare deleting one of the driver's feedback. Nothing changes until the driver presses "
            + "Confirm.")
    public String prepareDeleteFeedback(@ToolParam(description = "the feedback id") Long feedbackId) {
        JsonNode feedback = core.feedback(feedbackId);
        String summary = "Delete feedback #" + feedbackId + " (" + feedback.path("rating").asInt() + " star(s)): \""
                + excerpt(Data.text(feedback, "feedbackText")) + "\"";
        return pendingActions.prepare("deleteFeedback", new IdArgs(feedbackId), summary);
    }

    @Override
    public Map<String, Function<String, ActionResult>> handlers() {
        return Map.of(
                "book", this::book,
                "cancelBooking", json -> {
                    Long id = pendingActions.args(json, IdArgs.class).id();
                    core.cancelBooking(id);
                    return ActionResult.done("Booking #" + id + " is cancelled.");
                },
                "submitFeedback", json -> {
                    FeedbackArgs args = pendingActions.args(json, FeedbackArgs.class);
                    JsonNode saved = core.createFeedback(Map.of("sessionId", args.sessionId(), "rating", args.rating(),
                            "feedbackText", args.text()));
                    return ActionResult.done("Thank you - feedback #" + saved.path("feedbackId").asLong()
                            + " is saved.");
                },
                "deleteFeedback", json -> {
                    Long id = pendingActions.args(json, IdArgs.class).id();
                    core.deleteFeedback(id);
                    return ActionResult.done("Feedback #" + id + " is deleted.");
                });
    }

    // the same order as "Pay & Register"; angular pays it and confirms like the booking page
    private ActionResult book(String json) {
        Map<String, Object> request = bookingRequest(pendingActions.args(json, BookArgs.class));
        JsonNode order = payments.order(request);
        return ActionResult.payment("The payment window is opening for " + Data.text(order, "description")
                + ". The booking is made as soon as the payment goes through.", order, request);
    }

    // the booking form; the driver accepted the terms on the card
    private static Map<String, Object> bookingRequest(BookArgs args) {
        Map<String, Object> request = new HashMap<>();
        request.put("sessionId", args.sessionId());
        request.put("carNumber", args.carNumber());
        request.put("carCategory", args.carCategory());
        request.put("comments", args.comments());
        request.put("termsAccepted", true);
        return request;
    }

    private static LocalDate parseDate(String date) {
        try {
            return LocalDate.parse(date.trim());
        } catch (DateTimeParseException | NullPointerException e) {
            throw new ToolCallException(400, "The date must look like 2026-10-25");
        }
    }

    private static String excerpt(String text) {
        return Optional.ofNullable(text)
                .map(String::trim)
                .map(clean -> clean.length() > 80 ? clean.substring(0, 80) + "..." : clean)
                .orElse("");
    }
}
