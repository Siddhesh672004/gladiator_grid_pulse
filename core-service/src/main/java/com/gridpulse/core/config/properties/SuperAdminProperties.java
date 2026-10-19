package com.gridpulse.core.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

// the super admin from secrets.properties (SUPER_ADMIN_EMAIL / SUPER_ADMIN_PASSWORD), read by SuperAdminSeeder
@ConfigurationProperties(prefix = "gridpulse.super-admin")
public record SuperAdminProperties(String email, String password) {

    public boolean isConfigured() {
        return email != null && !email.isBlank() && password != null && !password.isBlank();
    }

    @Override
    public String toString() {
        return "SuperAdminProperties[email=" + email + "]";
    }
}
