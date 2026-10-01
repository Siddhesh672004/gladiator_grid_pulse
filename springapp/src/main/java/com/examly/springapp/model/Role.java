package com.examly.springapp.model;

import java.util.Arrays;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonValue;

// the label is what the db, the jwt and angular see ("Admin")
// SUPER_ADMIN can do everything an admin can + manage admins
public enum Role {

    SUPER_ADMIN("SuperAdmin"),
    ADMIN("Admin"),
    USER("User");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    // json shows the label ("Admin"), angular compares with it
    @JsonValue
    public String getLabel() {
        return label;
    }

    // a super admin counts as an admin everywhere an admin is allowed
    public boolean isAdmin() {
        return this == ADMIN || this == SUPER_ADMIN;
    }

    // unknown text -> IllegalArgumentException (400)
    public static Role fromLabel(String text) {
        // no text counts as an empty label
        String cleaned = Optional.ofNullable(text)
                .map(String::trim)
                .orElse("");
        // capital or small letters do not matter
        return Arrays.stream(values())
                .filter(role -> role.label.equalsIgnoreCase(cleaned))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown role: " + cleaned));
    }
}
