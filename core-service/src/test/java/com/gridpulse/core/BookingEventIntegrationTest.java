package com.gridpulse.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import com.gridpulse.core.model.RaceSession;
import com.gridpulse.core.model.User;
import com.gridpulse.core.support.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.gridpulse.common.event.BookingDecided;
import com.gridpulse.common.ticket.TicketCodes;

// a decision is saved together with its event (outbox row); an approval carries the e-ticket code
class BookingEventIntegrationTest extends IntegrationTest {

    @Test
    void approvalWritesTheEventWithATicket() throws Exception {
        RaceSession session = data.practiceSession(5);
        User driver = data.driver();
        Long bookingId = book(driver, session, 31);
        String admin = data.token(data.admin());

        assertThat(status(approve(admin, bookingId))).isEqualTo(200);

        BookingDecided decided = decisionOf(bookingId);
        assertThat(decided.status()).isEqualTo("Approved");
        assertThat(decided.driverEmail()).isEqualTo(driver.getEmail());
        assertThat(decided.trackName()).isEqualTo(session.getTrack().getTrackName());
        assertThat(TicketCodes.verify(decided.ticketCode(), TICKET_SECRET)).contains(bookingId);

        // the driver downloads the pdf, an admin checks the code at the gate
        ResponseEntity<byte[]> pdf = download("/api/race-booking/" + bookingId + "/ticket", data.token(driver));
        assertThat(status(pdf)).isEqualTo(200);
        assertThat(new String(pdf.getBody(), 0, 5)).isEqualTo("%PDF-");
        assertThat(pdf.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .contains("gridpulse-ticket-" + bookingId + ".pdf");
        JsonNode check = json(get("/api/tickets/verify/" + decided.ticketCode(), admin));
        assertThat(check.get("valid").asBoolean()).isTrue();
        assertThat(check.get("carNumber").asInt()).isEqualTo(31);
        assertThat(json(get("/api/tickets/verify/GP-" + bookingId + "-0000000000000000", admin)).get("valid")
                .asBoolean()).isFalse();
        // somebody else's ticket stays closed
        assertThat(status(download("/api/race-booking/" + bookingId + "/ticket", data.token(data.driver()))))
                .isEqualTo(403);
    }

    @Test
    void rejectionHasNoTicket() throws Exception {
        RaceSession session = data.practiceSession(5);
        User driver = data.driver();
        Long bookingId = book(driver, session, 32);

        ResponseEntity<String> rejected = put("/api/race-booking/" + bookingId, data.token(data.admin()),
                Map.of("status", "Rejected", "adminNote", "Car not ready"));

        assertThat(status(rejected)).isEqualTo(200);
        BookingDecided decided = decisionOf(bookingId);
        assertThat(decided.status()).isEqualTo("Rejected");
        assertThat(decided.ticketCode()).isNull();
        assertThat(decided.refundAmount()).isEqualTo(500.0);
        assertThat(status(download("/api/race-booking/" + bookingId + "/ticket", data.token(driver)))).isEqualTo(400);
    }

    private BookingDecided decisionOf(Long bookingId) {
        return outboxEvents(BookingDecided.class).stream()
                .filter(event -> event.bookingId().equals(bookingId))
                .reduce((first, last) -> last)
                .orElseThrow();
    }
}
