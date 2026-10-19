package com.gridpulse.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import com.gridpulse.core.account.data.User;
import com.gridpulse.core.booking.data.RaceBooking;
import com.gridpulse.core.competition.dto.CompetitionRequest;
import com.gridpulse.core.session.data.RaceSession;
import com.gridpulse.core.support.IntegrationTest;
import com.gridpulse.core.track.data.RaceTrack;
import com.fasterxml.jackson.databind.JsonNode;

// a whole competition through the api: two drivers qualify, race, one retires -> their stats + the leaderboard
class StatsIntegrationTest extends IntegrationTest {

    private static final String CONTROL = "/api/competition-control/";

    @Test
    void finishedCompetitionGivesStatsAndLeaderboard() throws Exception {
        RaceTrack track = data.track();
        String admin = data.token(data.admin());
        LocalDate day = RaceSession.today().plusDays(2);
        JsonNode competition = json(post("/api/competitions", admin, new CompetitionRequest(track.getTrackId(),
                "Stats Cup " + track.getTrackId(), day, LocalTime.of(10, 0), LocalTime.of(11, 0), LocalTime.of(12, 0),
                LocalTime.of(13, 0), 1000.0)));
        long competitionId = competition.get("competitionId").asLong();
        long qualifyingId = competition.get("qualifyingSession").get("sessionId").asLong();
        long raceId = competition.get("raceSession").get("sessionId").asLong();

        User winner = data.driver();
        User retired = data.driver();
        RaceSession qualifying = new RaceSession();
        qualifying.setSessionId(qualifyingId);
        Long winnerEntry = book(winner, qualifying, 1);
        Long retiredEntry = book(retired, qualifying, 2);
        approve(admin, winnerEntry);
        approve(admin, retiredEntry);

        // qualifying: 3 laps (= the circuit limit) against 2
        ok(post(CONTROL + competitionId + "/qualifying/start", admin, null));
        laps(admin, winnerEntry, 3);
        laps(admin, retiredEntry, 2);
        ok(post(CONTROL + competitionId + "/qualifying/finish", admin, null));

        // race: the winner does 5 laps, the other one retires after 1
        ok(post(CONTROL + competitionId + "/race/start", admin, null));
        Long winnerCar = raceCarOf(raceId, winner);
        Long retiredCar = raceCarOf(raceId, retired);
        laps(admin, winnerCar, 5);
        laps(admin, retiredCar, 1);
        ok(post(CONTROL + "retire/" + retiredCar, admin, null));
        ok(post(CONTROL + competitionId + "/race/finish", admin, null));

        JsonNode mine = json(get("/api/stats/me", data.token(winner)));
        assertThat(mine.get("sessionsDriven").asLong()).isEqualTo(2);
        assertThat(mine.get("totalLaps").asLong()).isEqualTo(8);
        assertThat(mine.get("competitionsEntered").asLong()).isEqualTo(1);
        assertThat(mine.get("wins").asLong()).isEqualTo(1);
        assertThat(mine.get("podiums").asLong()).isEqualTo(1);
        assertThat(mine.get("dnfs").asLong()).isZero();
        assertThat(mine.get("bestRaceFinish").asInt()).isEqualTo(1);
        assertThat(mine.get("bestQualifying").asInt()).isEqualTo(1);
        assertThat(mine.get("lapsPerMonth")).hasSize(12);
        // the laps sit in the month of the competition (a competition next year is not in this year's chart)
        double lapsThisYear = StreamSupport.stream(mine.get("lapsPerMonth").spliterator(), false)
                .mapToDouble(point -> point.get("value").asDouble())
                .sum();
        assertThat(lapsThisYear).isEqualTo(day.getYear() == RaceSession.today().getYear() ? 8.0 : 0.0);

        JsonNode theirs = json(get("/api/stats/me", data.token(retired)));
        assertThat(theirs.get("dnfs").asLong()).isEqualTo(1);
        assertThat(theirs.get("wins").asLong()).isZero();
        assertThat(theirs.get("bestRaceFinish").isNull()).isTrue();
        assertThat(theirs.get("bestQualifying").asInt()).isEqualTo(2);

        // public, no token
        JsonNode board = json(get("/api/stats/leaderboard?limit=5", null));
        assertThat(board.get(0).get("username").asText()).isEqualTo(winner.getUsername());
        assertThat(board.get(0).get("position").asInt()).isEqualTo(1);
        assertThat(board.get(0).get("wins").asLong()).isEqualTo(1);
        assertThat(board.get(1).get("username").asText()).isEqualTo(retired.getUsername());
        assertThat(board.get(0).has("email")).isFalse();
        assertThat(status(get("/api/stats/leaderboard?limit=21", null))).isEqualTo(400);
        // a driver only, never an admin
        assertThat(status(get("/api/stats/me", admin))).isEqualTo(403);
    }

    private void laps(String admin, Long bookingId, int count) {
        for (int lap = 0; lap < count; lap++) {
            ok(post(CONTROL + "lap/" + bookingId, admin, null));
        }
    }

    // the race place made from the qualifying result
    private Long raceCarOf(long raceSessionId, User driver) {
        return data.bookingsOf(raceSessionId).stream()
                .filter(car -> car.getUser().getUserId() == driver.getUserId())
                .map(RaceBooking::getRaceBookingId)
                .findFirst()
                .orElseThrow();
    }

    private static void ok(ResponseEntity<String> response) {
        assertThat(status(response)).as(response.getBody()).isEqualTo(200);
    }
}
