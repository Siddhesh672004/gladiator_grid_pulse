package com.examly.springapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import com.examly.springapp.support.IntegrationTest;

// new admin: invite email with a one-time link -> chooses the password -> logs in
class AdminInviteIntegrationTest extends IntegrationTest {

    private static final Pattern TOKEN = Pattern.compile("/set-password\\?email=\\S+&token=([A-Za-z0-9_-]+)");
    private static final String PASSWORD = "Pit#Wall2026";

    @Test
    void inviteLinkSetsThePasswordOnce() throws Exception {
        String superAdmin = data.token(data.superAdmin());
        String email = data.newEmail("invited");

        ResponseEntity<String> added = post("/api/super-admin/admins", superAdmin,
                Map.of("username", "Invited Admin", "email", email));
        assertThat(status(added)).isEqualTo(201);

        AtomicReference<String> mail = new AtomicReference<>();
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            List<String> mails = mailsTo(email);
            assertThat(mails).hasSize(1);
            mail.set(mails.get(0));
        });
        Matcher link = TOKEN.matcher(mail.get());
        assertThat(link.find()).isTrue();
        assertThat(mail.get()).doesNotContainIgnoringCase("password   :");
        String token = link.group(1);

        // no password chosen yet: nobody can log in
        assertThat(status(post("/api/admin/login", null, Map.of("email", email, "password", PASSWORD))))
                .isEqualTo(401);

        Map<String, String> setPassword = Map.of("email", email, "token", token, "newPassword", PASSWORD);
        assertThat(status(post("/api/password/set-initial", null, setPassword))).isEqualTo(200);
        ResponseEntity<String> again = post("/api/password/set-initial", null, setPassword);
        assertThat(status(again)).isEqualTo(400);
        assertThat(json(again).get("message").asText()).contains("already used");

        ResponseEntity<String> login = post("/api/admin/login", null, Map.of("email", email, "password", PASSWORD));
        assertThat(status(login)).isEqualTo(200);
        assertThat(json(login).get("token").asText()).isNotBlank();
    }

    @Test
    void resendWaitsAndClosesTheOldLink() throws Exception {
        String superAdmin = data.token(data.superAdmin());
        String email = data.newEmail("resend");
        long adminId = json(post("/api/super-admin/admins", superAdmin,
                Map.of("username", "Resend Admin", "email", email))).get("userId").asLong();

        // the first invite went out a moment ago
        ResponseEntity<String> tooSoon = post("/api/super-admin/admins/" + adminId + "/resend-invite", superAdmin, null);
        assertThat(status(tooSoon)).isEqualTo(400);
        // an admin may not resend invites
        String admin = data.token(data.admin());
        assertThat(status(post("/api/super-admin/admins/" + adminId + "/resend-invite", admin, null))).isEqualTo(403);
    }
}
