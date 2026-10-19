package com.gridpulse.ai;

import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import com.gridpulse.ai.copilot.data.ActionLogRepo;
import com.gridpulse.ai.copilot.data.PendingAction;
import com.gridpulse.ai.copilot.data.PendingActionRepo;
import com.gridpulse.ai.copilot.tools.AdminTools;
import com.gridpulse.ai.copilot.tools.Toolsets;
import com.gridpulse.ai.support.AiIntegrationTest;

// the Copilot without a model: which tools a role gets, prepare writes nothing, and the Confirm rules
class CopilotIntegrationTest extends AiIntegrationTest {

    private static final String BOOKING = """
            {"raceBookingId": 7, "user": {"username": "Aarav"}, "carNumber": 27, "status": "Pending",
             "paymentStatus": "PAID", "totalPrice": 650.0,
             "session": {"trackName": "Buddh", "sessionDate": "2026-11-01", "startTime": "10:00:00", "endTime": "11:00:00"}}""";

    @Autowired
    private Toolsets toolsets;

    @Autowired
    private AdminTools adminTools;

    @Autowired
    private PendingActionRepo pendingActionRepo;

    @Autowired
    private ActionLogRepo actionLogRepo;

    @Test
    void eachRoleGetsOnlyItsTools() {
        List<String> driver = toolNames("User");
        List<String> admin = toolNames("Admin");
        List<String> superAdmin = toolNames("SuperAdmin");

        assertThat(driver).contains("prepareBook", "myBookings").doesNotContain("prepareDecideBooking",
                "bookings", "prepareAddAdmin");
        assertThat(admin).contains("prepareDecideBooking", "bookings").doesNotContain("prepareBook",
                "prepareAddAdmin");
        assertThat(superAdmin).contains("prepareDecideBooking", "prepareAddAdmin", "listAdmins");
        // never exposed to anybody
        assertThat(superAdmin).noneMatch(name -> name.contains("Lap") || name.contains("Password")
                || name.contains("DeleteAdmin"));
    }

    @Test
    void prepareSavesACardAndChangesNothing() {
        actAs(900, "Admin");
        SERVICES.stubFor(get(urlEqualTo("/api/race-booking/7")).willReturn(okJson(BOOKING)));

        String answer = adminTools.prepareDecideBooking(7L, "REJECT", "No license shown");

        assertThat(answer).startsWith("Prepared. The user must press Confirm: Reject booking #7 of Aarav")
                .contains("Rs.650 paid is refunded");
        PendingAction card = pendingActionRepo.findAll().stream()
                .filter(action -> action.getUserId() == 900)
                .findFirst()
                .orElseThrow();
        assertThat(card.getRole()).isEqualTo("Admin");
        assertThat(card.getUsedAt()).isNull();
        assertThat(card.getExpiresAt()).isAfter(Instant.now().plus(4, ChronoUnit.MINUTES));
        // only read: no write reached the core
        SERVICES.verify(0, putRequestedFor(anyUrl()));
        SERVICES.verify(0, postRequestedFor(anyUrl()));
        SERVICES.verify(0, deleteRequestedFor(anyUrl()));
    }

    @Test
    void confirmIsForItsOwnerOnceWithTheirToken() {
        PendingAction card = card(910, "Admin", Instant.now().plus(5, ChronoUnit.MINUTES));
        SERVICES.stubFor(put(urlEqualTo("/api/race-booking/7")).willReturn(okJson(
                "{\"raceBookingId\": 7, \"status\": \"Rejected\", \"paymentStatus\": \"REFUND_PENDING\"}")));
        String adminToken = token(910, "Admin");

        assertThat(status(confirm(card, token(911, "Admin")))).isEqualTo(403);
        ResponseEntity<String> done = confirm(card, adminToken);
        assertThat(status(done)).isEqualTo(200);
        assertThat(done.getBody()).contains("\"ok\":true").contains("Rejected");
        // the core got the admin's own token and the saved decision
        SERVICES.verify(putRequestedFor(urlEqualTo("/api/race-booking/7"))
                .withHeader("Authorization", equalTo("Bearer " + adminToken))
                .withRequestBody(equalToJson("{\"status\": \"Rejected\", \"adminNote\": \"No license shown\"}")));
        // a second click does nothing
        assertThat(status(confirm(card, adminToken))).isEqualTo(409);
        SERVICES.verify(1, putRequestedFor(urlEqualTo("/api/race-booking/7")));
        assertThat(actionLogRepo.findAll()).anyMatch(log -> log.getUserId() == 910 && "OK".equals(log.getOutcome()));
    }

    @Test
    void anExpiredOrCancelledCardIsDead() {
        PendingAction late = card(920, "Admin", Instant.now().minus(1, ChronoUnit.MINUTES));
        PendingAction cancelled = card(920, "Admin", Instant.now().plus(5, ChronoUnit.MINUTES));
        String adminToken = token(920, "Admin");

        assertThat(status(confirm(late, adminToken))).isEqualTo(410);
        assertThat(status(post("/api/ai/copilot/actions/" + cancelled.getToken() + "/cancel", adminToken, null)))
                .isEqualTo(204);
        assertThat(status(confirm(cancelled, adminToken))).isEqualTo(409);
        SERVICES.verify(0, putRequestedFor(anyUrl()));
        assertThat(actionLogRepo.findAll()).extracting("outcome").contains("EXPIRED", "CANCELLED");
    }

    private List<String> toolNames(String role) {
        return Arrays.stream(ToolCallbacks.from(toolsets.forRole(role)))
                .map(callback -> callback.getToolDefinition().name())
                .toList();
    }

    // a prepared reject of booking 7, as prepareDecideBooking saves it
    private PendingAction card(int userId, String role, Instant expiresAt) {
        return pendingActionRepo.save(new PendingAction(UUID.randomUUID().toString().replace("-", ""), userId, role,
                "decideBooking", "{\"id\": 7, \"body\": {\"status\": \"Rejected\", \"adminNote\": \"No license shown\"}}",
                "Reject booking #7", Instant.now().minus(1, ChronoUnit.MINUTES), expiresAt));
    }

    private ResponseEntity<String> confirm(PendingAction card, String token) {
        return post("/api/ai/copilot/actions/" + card.getToken() + "/confirm", token, null);
    }
}
