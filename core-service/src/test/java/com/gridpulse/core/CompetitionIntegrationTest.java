package com.gridpulse.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import com.gridpulse.core.competition.dto.CompetitionRequest;
import com.gridpulse.core.session.data.RaceSession;
import com.gridpulse.core.support.IntegrationTest;
import com.gridpulse.core.track.data.RaceTrack;

// competition buttons only work in their own status
class CompetitionIntegrationTest extends IntegrationTest {

    @Test
    void competitionGuard() throws Exception {
        RaceTrack track = data.track();
        String admin = data.token(data.admin());
        CompetitionRequest request = new CompetitionRequest(track.getTrackId(), "Guard Cup " + track.getTrackId(),
                RaceSession.today().plusDays(2), LocalTime.of(10, 0), LocalTime.of(11, 0), LocalTime.of(12, 0),
                LocalTime.of(13, 0), 1000.0);

        ResponseEntity<String> created = post("/api/competitions", admin, request);
        assertThat(status(created)).isEqualTo(201);
        long competitionId = json(created).get("competitionId").asLong();

        // the race can't start before qualifying is done
        ResponseEntity<String> start = post("/api/competition-control/" + competitionId + "/race/start", admin, null);
        assertThat(status(start)).isEqualTo(400);
        assertThat(json(start).get("message").asText()).contains("UPCOMING");
        assertThat(json(get("/api/competitions/" + competitionId, null)).get("status").asText()).isEqualTo("UPCOMING");
    }
}
