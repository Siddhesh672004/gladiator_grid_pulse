package com.gridpulse.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.gridpulse.core.model.User;
import com.gridpulse.core.support.IntegrationTest;

// no token = 401, wrong role = 403, a switched off account's token = 401
class SecurityIntegrationTest extends IntegrationTest {

    private static final String ADMIN_ONLY = "/api/race-booking";

    @Test
    void security() {
        User driver = data.driver();
        User disabled = data.driver();
        String disabledToken = data.token(disabled);
        data.disable(disabled);

        assertThat(status(get(ADMIN_ONLY, null))).isEqualTo(401);
        assertThat(status(get(ADMIN_ONLY, data.token(driver)))).isEqualTo(403);
        assertThat(status(get(ADMIN_ONLY, disabledToken))).isEqualTo(401);
    }
}
