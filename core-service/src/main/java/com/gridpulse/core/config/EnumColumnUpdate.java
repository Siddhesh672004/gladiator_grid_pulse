package com.gridpulse.core.config;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.gridpulse.core.model.OtpPurpose;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// mysql keeps an enum as enum('A','B') and ddl-auto=update never changes a column:
// an older database gets the new value (OtpPurpose.ADMIN_INVITE) added once here, a new one already has it
@Slf4j
@Component
@RequiredArgsConstructor
public class EnumColumnUpdate {

    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void update() {
        String values = Arrays.stream(OtpPurpose.values())
                .map(purpose -> "'" + purpose.name() + "'")
                .collect(Collectors.joining(","));
        // only an enum(...) column that misses a value (mysql keeps the values as written: upper case)
        Optional.ofNullable(columnType("otp_code", "purpose"))
                .filter(type -> type.toLowerCase().startsWith("enum("))
                .filter(type -> Arrays.stream(OtpPurpose.values()).anyMatch(p -> !type.contains("'" + p.name() + "'")))
                .ifPresent(type -> {
                    jdbcTemplate.execute("ALTER TABLE otp_code MODIFY purpose ENUM(" + values + ") NOT NULL");
                    log.warn("otp_code.purpose now takes {}", values);
                });
    }

    private String columnType(String table, String column) {
        return jdbcTemplate.queryForList(
                "SELECT COLUMN_TYPE FROM information_schema.COLUMNS"
                        + " WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                String.class, table, column).stream()
                .findFirst()
                .orElse(null);
    }
}
