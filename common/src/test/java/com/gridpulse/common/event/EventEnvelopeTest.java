package com.gridpulse.common.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

class EventEnvelopeTest {

    private final EventReader reader = new EventReader();

    @Test
    void envelopeRoundTrip() throws Exception {
        BookingDecided decided = new BookingDecided(7L, "driver@gridpulse.test", "Aarav Rao", "Buddh Circuit",
                "Greater Noida", LocalDate.of(2026, 10, 20), LocalTime.of(10, 0), LocalTime.of(11, 0), 27, "GT3",
                "Approved", null, null, "GP-7-0123456789abcdef");
        EventEnvelope sent = new EventEnvelope("6f1c2a4e-0000-4000-8000-000000000001", "BookingDecided",
                Instant.parse("2026-10-15T10:00:00Z"), "springapp", EventJson.MAPPER.valueToTree(decided));
        String json = EventJson.MAPPER.writeValueAsString(sent);

        EventEnvelope received = reader.read(json);

        assertThat(json).contains("\"sessionDate\":\"2026-10-20\"");
        assertThat(received.eventId()).isEqualTo(sent.eventId());
        assertThat(received.type()).isEqualTo("BookingDecided");
        assertThat(received.occurredAt()).isEqualTo(sent.occurredAt());
        assertThat(reader.payload(received, BookingDecided.class)).isEqualTo(decided);
    }

    @Test
    void newerFieldsAreIgnoredAndJunkIsRefused() {
        String newer = "{\"eventId\":\"e1\",\"type\":\"OtpRequested\",\"occurredAt\":\"2026-10-15T10:00:00Z\","
                + "\"source\":\"springapp\",\"data\":{\"email\":\"a@b.co\",\"username\":\"A\",\"code\":\"123456\","
                + "\"minutes\":10,\"purpose\":\"SIGNUP\",\"language\":\"en\"},\"traceId\":\"x\"}";

        OtpRequested otp = reader.payload(reader.read(newer), OtpRequested.class);

        assertThat(otp.purpose()).isEqualTo("SIGNUP");
        assertThatThrownBy(() -> reader.read("not json")).isInstanceOf(IllegalArgumentException.class);
    }
}
