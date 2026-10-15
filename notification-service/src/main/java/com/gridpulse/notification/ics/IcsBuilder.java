package com.gridpulse.notification.ics;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import com.gridpulse.common.event.BookingDecided;

// the .ics of an approved booking (RFC 5545): one event in India time, opens in google / outlook / apple calendar
public final class IcsBuilder {

    private static final DateTimeFormatter LOCAL = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
    private static final DateTimeFormatter UTC = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
            .withZone(ZoneOffset.UTC);
    private static final int MAX_OCTETS = 75;

    private IcsBuilder() {
    }

    public static String build(BookingDecided booking, Instant now) {
        String where = booking.trackLocation() == null
                ? booking.trackName()
                : booking.trackName() + ", " + booking.trackLocation();
        List<String> lines = List.of(
                "BEGIN:VCALENDAR",
                "VERSION:2.0",
                "PRODID:-//GridPulse//EN",
                "CALSCALE:GREGORIAN",
                "METHOD:PUBLISH",
                // india has no summer time: one fixed +05:30 rule
                "BEGIN:VTIMEZONE",
                "TZID:Asia/Kolkata",
                "BEGIN:STANDARD",
                "DTSTART:19700101T000000",
                "TZOFFSETFROM:+0530",
                "TZOFFSETTO:+0530",
                "TZNAME:IST",
                "END:STANDARD",
                "END:VTIMEZONE",
                "BEGIN:VEVENT",
                "UID:booking-" + booking.bookingId() + "@gridpulse",
                "DTSTAMP:" + UTC.format(now),
                "DTSTART;TZID=Asia/Kolkata:" + LOCAL.format(booking.sessionDate().atTime(booking.startTime())),
                "DTEND;TZID=Asia/Kolkata:" + LOCAL.format(booking.sessionDate().atTime(booking.endTime())),
                "SUMMARY:" + escape("GridPulse: car #" + booking.carNumber() + " at " + booking.trackName()),
                "LOCATION:" + escape(where),
                "DESCRIPTION:" + escape("Booking " + booking.bookingId() + ", car #" + booking.carNumber() + " ("
                        + booking.carCategory() + "). Show the QR of your e-ticket at the gate."),
                "END:VEVENT",
                "END:VCALENDAR");
        return lines.stream()
                .map(IcsBuilder::fold)
                .collect(Collectors.joining("\r\n", "", "\r\n"));
    }

    // a text value: backslash, comma, semicolon and new lines get a backslash
    static String escape(String text) {
        return text.replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\r\n", "\\n")
                .replace("\n", "\\n");
    }

    // a line longer than 75 octets goes on in the next line, which starts with one space (never inside a character)
    static String fold(String line) {
        StringBuilder folded = new StringBuilder();
        int octets = 0;
        for (int i = 0; i < line.length(); ) {
            int codePoint = line.codePointAt(i);
            String character = new String(Character.toChars(codePoint));
            int size = character.getBytes(StandardCharsets.UTF_8).length;
            if (octets + size > MAX_OCTETS) {
                folded.append("\r\n ");
                octets = 1;
            }
            folded.append(character);
            octets += size;
            i += Character.charCount(codePoint);
        }
        return folded.toString();
    }
}
