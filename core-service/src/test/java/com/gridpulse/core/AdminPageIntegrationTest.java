package com.gridpulse.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import com.gridpulse.core.model.RaceSession;
import com.gridpulse.core.model.RaceTrack;
import com.gridpulse.core.support.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;

import jakarta.persistence.EntityManagerFactory;

// admin list pages come from the database one page at a time, with a fixed number of queries
class AdminPageIntegrationTest extends IntegrationTest {

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void sessionsPageWithFewQueries() throws Exception {
        RaceTrack track = data.track();
        List<RaceSession> sessions = new ArrayList<>();
        for (int day = 1; day <= 25; day++) {
            sessions.add(data.practiceSession(track, day, 5));
        }
        RaceSession firstDay = sessions.get(0);
        String admin = data.token(data.admin());
        assertThat(status(approve(admin, book(data.driver(), firstDay, 41)))).isEqualTo(200);

        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        ResponseEntity<String> page = get("/api/sessions/page?search=" + track.getTrackName() + "&size=25", admin);
        long statements = statistics.getPrepareStatementCount();

        assertThat(status(page)).isEqualTo(200);
        JsonNode body = json(page);
        assertThat(body.get("totalElements").asLong()).isEqualTo(25);
        assertThat(body.get("content")).hasSize(25);
        // user of the token + rows + count + approved cars of the whole page
        assertThat(statements).isLessThanOrEqualTo(5);
        // newest date first: the first day is the last row and holds the approved car
        JsonNode last = body.get("content").get(24);
        assertThat(last.get("sessionId").asLong()).isEqualTo(firstDay.getSessionId());
        assertThat(last.get("approvedCars").asLong()).isEqualTo(1);
    }

    @Test
    void bookingPageFiltersByStatus() throws Exception {
        RaceSession session = data.practiceSession(5);
        book(data.driver(), session, 51);
        Long approved = book(data.driver(), session, 52);
        String admin = data.token(data.admin());
        approve(admin, approved);
        String trackName = session.getTrack().getTrackName();

        JsonNode pending = json(get("/api/race-booking/page?search=" + trackName + "&status=Pending", admin));
        JsonNode all = json(get("/api/race-booking/page?search=" + trackName + "&status=All&size=1", admin));

        assertThat(pending.get("totalElements").asLong()).isEqualTo(1);
        assertThat(pending.get("content").get(0).get("carNumber").asInt()).isEqualTo(51);
        assertThat(all.get("totalElements").asLong()).isEqualTo(2);
        // newest first
        assertThat(all.get("content").get(0).get("raceBookingId").asLong()).isEqualTo(approved);
        assertThat(status(get("/api/race-booking/page?status=Unknown", admin))).isEqualTo(400);
        assertThat(status(get("/api/race-booking/page?size=51", admin))).isEqualTo(400);
    }
}
