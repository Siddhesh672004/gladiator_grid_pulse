package com.gridpulse.ai.tools;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.StreamSupport;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.gridpulse.ai.client.CoreClient;
import com.gridpulse.ai.client.PaymentClient;
import com.gridpulse.ai.dto.response.ActionResult;
import com.gridpulse.ai.exceptions.ToolCallException;
import com.gridpulse.ai.security.annotation.AdminOnly;
import com.gridpulse.ai.service.FeedbackAiService;
import com.gridpulse.ai.service.PendingActions;

import lombok.RequiredArgsConstructor;

// race control's Copilot: reads every list of the admin pages; tracks, sessions, competitions, bookings, refunds
// and driver accounts change only as Confirm cards. never exposed: race control buttons, laps, pictures, passwords
// every call carries the admin's token, the core / payment-service check the role again
@Component
@RequiredArgsConstructor
public class AdminTools implements RoleTools, ConfirmableTools {

    private static final int PAGE_SIZE = 10;

    // the arguments a card keeps until Confirm: the full request, built and shown in the summary at prepare time
    private record RequestArgs(Long id, Map<String, Object> body) {
    }

    private record SwitchArgs(Long id, boolean on) {
    }

    private final CoreClient core;
    private final PaymentClient payments;
    private final FeedbackAiService feedbackAiService;
    private final PendingActions pendingActions;

    @Override
    public Set<String> roles() {
        return Set.of("Admin", "SuperAdmin");
    }

    @AdminOnly
    @Tool(description = "The admin dashboard numbers: users, bookings, tracks, live races, revenue, feedback.")
    public JsonNode dashboardSummary() {
        return core.dashboard().path("metrics");
    }

    @AdminOnly
    @Tool(description = "Money: collected, refunded, refunds in progress, paid bookings.")
    public JsonNode revenueSummary() {
        return Data.pick(payments.summary(), "collected", "refunded", "refundInProgress", "paidBookings");
    }

    @AdminOnly
    @Tool(description = "One page of bookings, newest first, optionally only one status.")
    public JsonNode bookings(
            @ToolParam(description = "Pending, Approved, Rejected or Waitlisted; empty = all", required = false)
            String status,
            @ToolParam(description = "page number from 0", required = false) Integer page) {
        return Data.page(core.bookingPage(status, pageOf(page), PAGE_SIZE), "raceBookingId", "user.username",
                "session.trackName", "session.sessionDate", "session.startTime", "status", "carNumber",
                "paymentStatus", "waitlistPosition");
    }

    @AdminOnly
    @Tool(description = "Everything about one booking: driver, session, car, status, payment, note.")
    public JsonNode bookingDetails(@ToolParam(description = "the booking id") Long bookingId) {
        return Data.pick(core.booking(bookingId), "raceBookingId", "user.username", "user.email",
                "user.mobileNumber", "session.sessionId", "session.trackName", "session.sessionDate",
                "session.startTime", "session.endTime", "session.status:sessionStatus", "status", "carNumber",
                "carCategory", "driverLicenseNumber", "comments", "adminNote", "totalPrice", "paymentStatus",
                "waitlistPosition");
    }

    @AdminOnly
    @Tool(description = "One page of sessions, newest first; optionally by status or track name.")
    public JsonNode sessions(
            @ToolParam(description = "SCHEDULED, LIVE, COMPLETED or CANCELLED; empty = all", required = false)
            String status,
            @ToolParam(description = "part of a track name", required = false) String search,
            @ToolParam(description = "page number from 0", required = false) Integer page) {
        return Data.page(core.sessionPage(search, status, pageOf(page), PAGE_SIZE), "sessionId", "trackName",
                "sessionType", "phase", "competitionName", "sessionDate", "startTime", "endTime", "price", "maxCars",
                "approvedCars", "status");
    }

    @AdminOnly
    @Tool(description = "All competitions with their qualifying and race session ids.")
    public JsonNode competitions() {
        return Data.pickAll(core.competitions(), "competitionId", "competitionName", "trackName", "competitionDate",
                "status", "qualifyingSession.sessionId:qualifyingSessionId", "raceSession.sessionId:raceSessionId",
                "qualifyingSession.price:entryFee");
    }

    @AdminOnly
    @Tool(description = "One page of feedback, newest first, with the AI label; optional filters.")
    public JsonNode feedback(
            @ToolParam(description = "POSITIVE, NEGATIVE, NEUTRAL or NOT_ANALYSED", required = false) String sentiment,
            @ToolParam(description = "stars 1 to 5", required = false) Integer rating,
            @ToolParam(description = "true = only the urgent ones", required = false) Boolean urgentOnly,
            @ToolParam(description = "page number from 0", required = false) Integer page) {
        return Data.page(core.feedbackPage(sentiment, Optional.ofNullable(rating).map(String::valueOf).orElse(null),
                Boolean.TRUE.equals(urgentOnly), pageOf(page), PAGE_SIZE), "feedbackId", "user.username",
                "session.trackName", "date", "rating", "feedbackText", "sentiment", "urgent");
    }

    @AdminOnly
    @Tool(description = "A three line AI summary of the newest feedback: liked, disliked, improve.")
    public List<String> feedbackSummary() {
        return feedbackAiService.summariseRecent();
    }

    @AdminOnly
    @Tool(description = "One page of driver accounts; optional search (name, email, mobile) and status.")
    public JsonNode drivers(
            @ToolParam(description = "part of a name, email or mobile number", required = false) String search,
            @ToolParam(description = "Active or Inactive; empty = all", required = false) String status,
            @ToolParam(description = "page number from 0", required = false) Integer page) {
        return Data.page(core.drivers(search, status, pageOf(page), PAGE_SIZE), "userId", "username", "email",
                "mobileNumber", "enabled", "profileComplete");
    }

    @AdminOnly
    @Tool(description = "One page of payments, newest first; e.g. status REFUND_FAILED to find refunds to retry.")
    public JsonNode payments(
            @ToolParam(description = "CREATED, CAPTURED, PAID, FAILED, REFUND_PENDING, REFUNDED or REFUND_FAILED",
                    required = false) String status,
            @ToolParam(description = "page number from 0", required = false) Integer page) {
        return Data.page(payments.page(status, pageOf(page), PAGE_SIZE), "paymentId", "user.username",
                "session.trackName", "session.sessionDate", "amount", "status", "razorpayOrderId", "note");
    }

    @AdminOnly
    @Tool(description = "The audit trail of one payment (order created, paid, booking, refund ...).")
    public JsonNode paymentTrail(@ToolParam(description = "the razorpay order id, order_...") String orderId) {
        return payments.trail(orderId);
    }

    @AdminOnly
    @Tool(description = "Check an e-ticket code (the text under the QR code).")
    public JsonNode checkTicket(@ToolParam(description = "the ticket code, GP-...") String code) {
        return core.checkTicket(code);
    }

    @AdminOnly
    @Tool(description = "Prepare adding a track. Needs 1 to 5 picture urls that were already uploaded; without them "
            + "tell the admin to use the Add Track page. Nothing changes until the admin presses Confirm.")
    public String prepareCreateTrack(
            @ToolParam(description = "track name") String trackName,
            @ToolParam(description = "city or address") String location,
            @ToolParam(description = "10 to 1000 characters") String description,
            @ToolParam(description = "Circuit, Karting, Rally or Drag Strip") String trackCategory,
            @ToolParam(description = "true = indoor") Boolean isIndoor,
            @ToolParam(description = "opening time, HH:mm") String openTime,
            @ToolParam(description = "closing time, HH:mm") String closeTime,
            @ToolParam(description = "facilities, e.g. Pit lane, Cafe") String facilities,
            @ToolParam(description = "1 to 5 urls of pictures already uploaded", required = false)
            List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty() || imageUrls.size() > 5) {
            return "Not possible here: a track needs 1 to 5 pictures. Ask the admin to add it on Tracks > Add Track, "
                    + "where the pictures are uploaded.";
        }
        Map<String, Object> body = new HashMap<>();
        body.put("trackName", trackName);
        body.put("location", location);
        body.put("description", description);
        body.put("trackCategory", trackCategory);
        body.put("isIndoor", isIndoor);
        body.put("openTime", openTime);
        body.put("closeTime", closeTime);
        body.put("facilities", facilities);
        body.put("imageUrls", imageUrls);
        String summary = "Add the " + (Boolean.TRUE.equals(isIndoor) ? "indoor" : "outdoor") + " " + trackCategory
                + " track \"" + trackName + "\" in " + location + ", open " + openTime + "-" + closeTime + ", "
                + imageUrls.size() + " picture(s).";
        return pendingActions.prepare("createTrack", new RequestArgs(null, body), summary);
    }

    @AdminOnly
    @Tool(description = "Prepare changing a track; only the fields given change. Nothing changes until the admin "
            + "presses Confirm.")
    public String prepareUpdateTrack(
            @ToolParam(description = "the track id") Long trackId,
            @ToolParam(description = "new name", required = false) String trackName,
            @ToolParam(description = "new location", required = false) String location,
            @ToolParam(description = "new description", required = false) String description,
            @ToolParam(description = "new category", required = false) String trackCategory,
            @ToolParam(description = "true = indoor", required = false) Boolean isIndoor,
            @ToolParam(description = "new opening time, HH:mm", required = false) String openTime,
            @ToolParam(description = "new closing time, HH:mm", required = false) String closeTime,
            @ToolParam(description = "new facilities", required = false) String facilities) {
        JsonNode track = core.track(trackId);
        Map<String, Object> body = new HashMap<>();
        body.put("trackName", or(trackName, Data.text(track, "trackName")));
        body.put("location", or(location, Data.text(track, "location")));
        body.put("description", or(description, Data.text(track, "description")));
        body.put("trackCategory", or(trackCategory, Data.text(track, "trackCategory")));
        body.put("isIndoor", Optional.ofNullable(isIndoor).orElse(track.path("isIndoor").asBoolean()));
        body.put("openTime", or(openTime, Data.hhmm(Data.text(track, "openTime"))));
        body.put("closeTime", or(closeTime, Data.hhmm(Data.text(track, "closeTime"))));
        body.put("facilities", or(facilities, Data.text(track, "facilities")));
        body.put("imageUrls", StreamSupport.stream(track.path("imageUrls").spliterator(), false)
                .map(JsonNode::asText)
                .toList());
        String summary = "Change track #" + trackId + " \"" + Data.text(track, "trackName") + "\": " + body.get(
                "trackName") + ", " + body.get("location") + ", " + body.get("trackCategory") + ", open "
                + body.get("openTime") + "-" + body.get("closeTime") + ".";
        return pendingActions.prepare("updateTrack", new RequestArgs(trackId, body), summary);
    }

    @AdminOnly
    @Tool(description = "Prepare showing (active) or hiding (inactive) a track for drivers. Nothing changes until "
            + "the admin presses Confirm.")
    public String prepareSetTrackActive(
            @ToolParam(description = "the track id") Long trackId,
            @ToolParam(description = "true = active, false = inactive") Boolean active) {
        JsonNode track = core.track(trackId);
        String summary = (Boolean.TRUE.equals(active) ? "Activate" : "Deactivate (hide from drivers)") + " track #"
                + trackId + " \"" + Data.text(track, "trackName") + "\".";
        return pendingActions.prepare("setTrackActive", new SwitchArgs(trackId, Boolean.TRUE.equals(active)), summary);
    }

    @AdminOnly
    @Tool(description = "Prepare deleting a track that never had a session. Nothing changes until the admin presses "
            + "Confirm.")
    public String prepareDeleteTrack(@ToolParam(description = "the track id") Long trackId) {
        JsonNode track = core.track(trackId);
        String summary = "Delete track #" + trackId + " \"" + Data.text(track, "trackName") + "\" for good.";
        return pendingActions.prepare("deleteTrack", new RequestArgs(trackId, Map.of()), summary);
    }

    @AdminOnly
    @Tool(description = "Prepare a new practice session. Nothing changes until the admin presses Confirm.")
    public String prepareCreatePracticeSession(
            @ToolParam(description = "the track id") Long trackId,
            @ToolParam(description = "date, yyyy-MM-dd") String date,
            @ToolParam(description = "start time, HH:mm") String startTime,
            @ToolParam(description = "end time, HH:mm") String endTime,
            @ToolParam(description = "price in rupees") Double price,
            @ToolParam(description = "most cars allowed") Integer maxCars) {
        JsonNode track = core.track(trackId);
        Map<String, Object> body = sessionBody(trackId, date, startTime, endTime, price, maxCars);
        String summary = "Add a practice session on " + Data.text(track, "trackName") + ", " + date + " " + startTime
                + "-" + endTime + ", " + Data.rupees(price) + ", up to " + maxCars + " cars.";
        return pendingActions.prepare("createSession", new RequestArgs(null, body), summary);
    }

    @AdminOnly
    @Tool(description = "Prepare changing a scheduled practice session; only the fields given change. Nothing changes "
            + "until the admin presses Confirm.")
    public String prepareUpdatePracticeSession(
            @ToolParam(description = "the session id") Long sessionId,
            @ToolParam(description = "new date, yyyy-MM-dd", required = false) String date,
            @ToolParam(description = "new start time, HH:mm", required = false) String startTime,
            @ToolParam(description = "new end time, HH:mm", required = false) String endTime,
            @ToolParam(description = "new price in rupees", required = false) Double price,
            @ToolParam(description = "new most cars allowed", required = false) Integer maxCars) {
        JsonNode session = core.session(sessionId);
        Map<String, Object> body = sessionBody(session.path("trackId").asLong(),
                or(date, Data.text(session, "sessionDate")),
                or(startTime, Data.hhmm(Data.text(session, "startTime"))),
                or(endTime, Data.hhmm(Data.text(session, "endTime"))),
                Optional.ofNullable(price).orElse(session.path("price").asDouble()),
                Optional.ofNullable(maxCars).orElse(session.path("maxCars").asInt()));
        String summary = "Change session #" + sessionId + " (" + Data.sessionText(session) + ") to " + body.get(
                "sessionDate") + " " + body.get("startTime") + "-" + body.get("endTime") + ", "
                + Data.rupees(((Number) body.get("price")).doubleValue()) + ", up to " + body.get("maxCars") + " cars.";
        return pendingActions.prepare("updateSession", new RequestArgs(sessionId, body), summary);
    }

    @AdminOnly
    @Tool(description = "Prepare cancelling a scheduled practice session: every booking is rejected and paid ones are "
            + "refunded. Nothing changes until the admin presses Confirm.")
    public String prepareCancelSession(@ToolParam(description = "the session id") Long sessionId) {
        JsonNode session = core.session(sessionId);
        String summary = "Cancel session #" + sessionId + " (" + Data.sessionText(session) + "). Every booking in it is "
                + "rejected and the paid ones are refunded; the drivers get an email.";
        return pendingActions.prepare("cancelSession", new RequestArgs(sessionId, Map.of()), summary);
    }

    @AdminOnly
    @Tool(description = "Prepare a competition (a qualifying and a race session on one day). Nothing changes until "
            + "the admin presses Confirm.")
    public String prepareCreateCompetition(
            @ToolParam(description = "the track id") Long trackId,
            @ToolParam(description = "competition name") String competitionName,
            @ToolParam(description = "date, yyyy-MM-dd") String date,
            @ToolParam(description = "qualifying start, HH:mm") String qualifyingStartTime,
            @ToolParam(description = "qualifying end, HH:mm") String qualifyingEndTime,
            @ToolParam(description = "race start, HH:mm") String raceStartTime,
            @ToolParam(description = "race end, HH:mm") String raceEndTime,
            @ToolParam(description = "entry fee in rupees") Double entryFee) {
        JsonNode track = core.track(trackId);
        Map<String, Object> body = new HashMap<>();
        body.put("trackId", trackId);
        body.put("competitionName", competitionName);
        body.put("competitionDate", date);
        body.put("qualifyingStartTime", qualifyingStartTime);
        body.put("qualifyingEndTime", qualifyingEndTime);
        body.put("raceStartTime", raceStartTime);
        body.put("raceEndTime", raceEndTime);
        body.put("entryFee", entryFee);
        String summary = "Add the competition \"" + competitionName + "\" on " + Data.text(track, "trackName") + ", "
                + date + ": qualifying " + qualifyingStartTime + "-" + qualifyingEndTime + ", race " + raceStartTime
                + "-" + raceEndTime + ", entry fee " + Data.rupees(entryFee) + ".";
        return pendingActions.prepare("createCompetition", new RequestArgs(null, body), summary);
    }

    @AdminOnly
    @Tool(description = "Prepare cancelling a competition before qualifying starts: its bookings are rejected and "
            + "refunded. Nothing changes until the admin presses Confirm.")
    public String prepareCancelCompetition(@ToolParam(description = "the competition id") Long competitionId) {
        JsonNode competition = core.competition(competitionId);
        String summary = "Cancel the competition \"" + Data.text(competition, "competitionName") + "\" ("
                + Data.text(competition, "trackName") + ", " + Data.text(competition, "competitionDate")
                + "). Its bookings are rejected and refunded.";
        return pendingActions.prepare("cancelCompetition", new RequestArgs(competitionId, Map.of()), summary);
    }

    @AdminOnly
    @Tool(description = "Prepare approving or rejecting a booking (a reject needs a reason of 5+ characters). Nothing "
            + "changes until the admin presses Confirm.")
    public String prepareDecideBooking(
            @ToolParam(description = "the booking id") Long bookingId,
            @ToolParam(description = "APPROVE or REJECT") String decision,
            @ToolParam(description = "the reason, needed to reject", required = false) String reason) {
        boolean approve = "APPROVE".equalsIgnoreCase(decision.trim());
        if (!approve && (reason == null || reason.trim().length() < 5)) {
            throw new ToolCallException(400, "A reject needs a reason of at least 5 characters - ask the admin for one");
        }
        JsonNode booking = core.booking(bookingId);
        Map<String, Object> body = new HashMap<>();
        body.put("status", approve ? "Approved" : "Rejected");
        Optional.ofNullable(reason).map(String::trim).filter(text -> !text.isEmpty())
                .ifPresent(text -> body.put("adminNote", text));
        String who = Data.text(booking, "user.username") + " (car #" + booking.path("carNumber").asInt() + ", "
                + Data.sessionText(booking.path("session")) + ")";
        String money = !approve && "PAID".equals(Data.text(booking, "paymentStatus"))
                ? " The " + Data.rupees(booking.path("totalPrice").asDouble()) + " paid is refunded."
                : "";
        String summary = (approve ? "Approve" : "Reject") + " booking #" + bookingId + " of " + who
                + (approve ? "." : " - reason: " + reason.trim() + ".") + money;
        return pendingActions.prepare("decideBooking", new RequestArgs(bookingId, body), summary);
    }

    @AdminOnly
    @Tool(description = "Prepare trying a failed refund again. Nothing changes until the admin presses Confirm.")
    public String prepareRetryRefund(@ToolParam(description = "the payment id") Long paymentId) {
        JsonNode failed = StreamSupport.stream(payments.page("REFUND_FAILED", 0, 50).path("content").spliterator(),
                        false)
                .filter(payment -> payment.path("paymentId").asLong() == paymentId)
                .findFirst()
                .orElseThrow(() -> new ToolCallException(400, "Payment #" + paymentId + " has no failed refund"));
        String summary = "Try the refund of payment #" + paymentId + " again: "
                + Data.rupees(failed.path("amount").asDouble()) + " back to " + Data.text(failed, "user.username") + ".";
        return pendingActions.prepare("retryRefund", new RequestArgs(paymentId, Map.of()), summary);
    }

    @AdminOnly
    @Tool(description = "Prepare enabling or disabling a driver account. Nothing changes until the admin presses "
            + "Confirm.")
    public String prepareSetDriverActive(
            @ToolParam(description = "the driver's user id") Integer userId,
            @ToolParam(description = "true = enabled, false = disabled") Boolean enabled) {
        String name = StreamSupport.stream(core.drivers(null, null, 0, 50).path("content").spliterator(), false)
                .filter(driver -> driver.path("userId").asInt() == userId)
                .map(driver -> Data.text(driver, "username") + " (" + Data.text(driver, "email") + ")")
                .findFirst()
                .orElse("#" + userId);
        String summary = (Boolean.TRUE.equals(enabled) ? "Enable" : "Disable (cannot log in)") + " the driver account "
                + name + ".";
        return pendingActions.prepare("setDriverActive", new SwitchArgs((long) userId, Boolean.TRUE.equals(enabled)),
                summary);
    }

    @Override
    public Map<String, Function<String, ActionResult>> handlers() {
        Map<String, Function<String, ActionResult>> handlers = new HashMap<>();
        handlers.put("createTrack", json -> {
            JsonNode track = core.createTrack(request(json).body());
            return ActionResult.done("Track #" + track.path("trackId").asLong() + " \"" + Data.text(track, "trackName")
                    + "\" is added.");
        });
        handlers.put("updateTrack", json -> {
            RequestArgs args = request(json);
            core.updateTrack(args.id(), args.body());
            return ActionResult.done("Track #" + args.id() + " is updated.");
        });
        handlers.put("setTrackActive", json -> {
            SwitchArgs args = pendingActions.args(json, SwitchArgs.class);
            JsonNode track = args.on() ? core.activateTrack(args.id()) : core.deactivateTrack(args.id());
            return ActionResult.done("Track \"" + Data.text(track, "trackName") + "\" is now "
                    + (args.on() ? "active." : "inactive."));
        });
        handlers.put("deleteTrack", json -> {
            Long id = request(json).id();
            core.deleteTrack(id);
            return ActionResult.done("Track #" + id + " is deleted.");
        });
        handlers.put("createSession", json -> {
            JsonNode session = core.createSession(request(json).body());
            return ActionResult.done("Session #" + session.path("sessionId").asLong() + " is added ("
                    + Data.sessionText(session) + ").");
        });
        handlers.put("updateSession", json -> {
            RequestArgs args = request(json);
            JsonNode session = core.updateSession(args.id(), args.body());
            return ActionResult.done("Session #" + args.id() + " is updated (" + Data.sessionText(session) + ").");
        });
        handlers.put("cancelSession", json -> {
            Long id = request(json).id();
            core.cancelSession(id);
            return ActionResult.done("Session #" + id + " is cancelled; its bookings are rejected and refunded.");
        });
        handlers.put("createCompetition", json -> {
            JsonNode competition = core.createCompetition(request(json).body());
            return ActionResult.done("Competition #" + competition.path("competitionId").asLong() + " \""
                    + Data.text(competition, "competitionName") + "\" is added.");
        });
        handlers.put("cancelCompetition", json -> {
            Long id = request(json).id();
            core.cancelCompetition(id);
            return ActionResult.done("Competition #" + id + " is cancelled; its bookings are rejected and refunded.");
        });
        handlers.put("decideBooking", json -> {
            RequestArgs args = request(json);
            JsonNode booking = core.decideBooking(args.id(), args.body());
            return ActionResult.done("Booking #" + args.id() + " is now " + Data.text(booking, "status")
                    + paymentNote(booking));
        });
        handlers.put("retryRefund", json -> {
            Long id = request(json).id();
            JsonNode payment = payments.retryRefund(id);
            return ActionResult.done("The refund of payment #" + id + " is tried again (" + Data.text(payment, "status")
                    + ").");
        });
        handlers.put("setDriverActive", json -> {
            SwitchArgs args = pendingActions.args(json, SwitchArgs.class);
            core.setDriverStatus(args.id().intValue(), Map.of("enabled", args.on()));
            return ActionResult.done("The driver account #" + args.id() + " is now "
                    + (args.on() ? "enabled." : "disabled."));
        });
        return handlers;
    }

    private RequestArgs request(String json) {
        return pendingActions.args(json, RequestArgs.class);
    }

    private static Map<String, Object> sessionBody(Long trackId, String date, String startTime, String endTime,
            Double price, Integer maxCars) {
        Map<String, Object> body = new HashMap<>();
        body.put("trackId", trackId);
        body.put("sessionDate", date);
        body.put("startTime", startTime);
        body.put("endTime", endTime);
        body.put("price", price);
        body.put("maxCars", maxCars);
        return body;
    }

    private static String paymentNote(JsonNode booking) {
        return "REFUND_PENDING".equals(Data.text(booking, "paymentStatus"))
                ? "; the payment is being refunded."
                : ".";
    }

    private static int pageOf(Integer page) {
        return Optional.ofNullable(page).filter(number -> number >= 0).orElse(0);
    }

    private static String or(String given, String current) {
        return Optional.ofNullable(given).map(String::trim).filter(text -> !text.isEmpty()).orElse(current);
    }
}
