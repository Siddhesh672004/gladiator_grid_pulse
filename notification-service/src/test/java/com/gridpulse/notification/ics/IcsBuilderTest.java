package com.gridpulse.notification.ics;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.gridpulse.common.event.BookingDecided;

class IcsBuilderTest {

    private static final BookingDecided BOOKING = new BookingDecided(7L, "driver@gridpulse.test", "Aarav Rao",
            "Buddh International Circuit", "Greater Noida, Uttar Pradesh; India", LocalDate.of(2026, 10, 20),
            LocalTime.of(10, 0), LocalTime.of(11, 30), 27, "GT3", "Approved", null, null, "GP-7-0123456789abcdef");

    @Test
    void oneEventInIndiaTime() {
        String ics = IcsBuilder.build(BOOKING, Instant.parse("2026-10-15T08:30:00Z"));

        assertThat(ics).startsWith("BEGIN:VCALENDAR\r\nVERSION:2.0\r\nPRODID:-//GridPulse//EN\r\n");
        assertThat(ics).endsWith("END:VCALENDAR\r\n");
        assertThat(ics).contains("METHOD:PUBLISH", "TZID:Asia/Kolkata", "TZOFFSETTO:+0530", "UID:booking-7@gridpulse",
                "DTSTAMP:20261015T083000Z", "DTSTART;TZID=Asia/Kolkata:20261020T100000",
                "DTEND;TZID=Asia/Kolkata:20261020T113000", "SUMMARY:GridPulse: car #27 at Buddh International Circuit");
        // only CRLF line ends
        assertThat(ics.replace("\r\n", "")).doesNotContain("\n").doesNotContain("\r");
    }

    @Test
    void textIsEscapedAndLongLinesFolded() {
        String ics = IcsBuilder.build(BOOKING, Instant.parse("2026-10-15T08:30:00Z"));
        String unfolded = ics.replace("\r\n ", "");

        assertThat(unfolded).contains("LOCATION:Buddh International Circuit\\, Greater Noida\\, Uttar Pradesh\\; India");
        assertThat(Arrays.stream(ics.split("\r\n")))
                .allMatch(line -> line.getBytes(StandardCharsets.UTF_8).length <= 75);
        assertThat(IcsBuilder.escape("a\\b,c;d\ne")).isEqualTo("a\\\\b\\,c\\;d\\ne");
    }

    @Test
    void foldingNeverSplitsACharacter() {
        String line = "SUMMARY:" + "ä".repeat(60);

        String folded = IcsBuilder.fold(line);

        assertThat(folded.replace("\r\n ", "")).isEqualTo(line);
        assertThat(Arrays.stream(folded.split("\r\n")))
                .allMatch(part -> part.getBytes(StandardCharsets.UTF_8).length <= 75);
    }
}
