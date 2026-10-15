package com.examly.springapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import com.examly.springapp.model.RaceSession;
import com.examly.springapp.model.User;
import com.examly.springapp.support.IntegrationTest;

// the approval answers at once, the email comes a moment later from the email thread
class EmailEventIntegrationTest extends IntegrationTest {

    @Test
    void approvingABookingWritesTheEmail() throws Exception {
        RaceSession session = data.practiceSession(5);
        User driver = data.driver();
        Long bookingId = book(driver, session, 31);

        assertThat(status(approve(data.token(data.admin()), bookingId))).isEqualTo(200);

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(mailsTo(driver.getEmail()))
                .anySatisfy(text -> assertThat(text).contains("Subject: Your GridPulse booking was approved")));
    }
}
