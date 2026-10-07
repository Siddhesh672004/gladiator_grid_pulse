package com.examly.springapp.config;

import java.util.List;
import java.util.Optional;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// one-time clean up of an old database: deletes bookings / feedback of the old layout and drops the unused columns
// (ddl-auto=update never removes anything); does nothing once the old columns are gone
@Slf4j
@Component
@RequiredArgsConstructor
public class OldScheduleCleanup {

    private static final List<String> OLD_BOOKING_COLUMNS = List.of("track_id", "race_date", "start_time",
            "end_time", "session_type");
    private static final List<String> OLD_FEEDBACK_COLUMNS = List.of("track_id", "track_name", "active");

    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void cleanUp() {
        ifColumnExists("race_booking", "race_date", this::cleanBookings);
        ifColumnExists("feedback", "track_id", this::cleanFeedback);
        ifColumnExists("race_track", "price_per_session", this::dropTrackPrice);
    }

    private void cleanBookings() {
        int deleted = jdbcTemplate.update("DELETE FROM race_booking");
        OLD_BOOKING_COLUMNS.forEach(column -> dropColumn("race_booking", column));
        jdbcTemplate.execute("ALTER TABLE race_booking MODIFY comments VARCHAR(500)");
        log.warn("Old booking layout found: {} old booking(s) deleted, old columns removed", deleted);
    }

    private void cleanFeedback() {
        int deleted = jdbcTemplate.update("DELETE FROM feedback");
        OLD_FEEDBACK_COLUMNS.forEach(column -> dropColumn("feedback", column));
        log.warn("Old feedback layout found: {} old feedback row(s) deleted, old columns removed", deleted);
    }

    private void dropTrackPrice() {
        dropColumn("race_track", "price_per_session");
        log.warn("Old track price column removed (every session has its own price)");
    }

    private void ifColumnExists(String table, String column, Runnable work) {
        // the work runs only while the column is still in the table
        Optional.of(column)
                .filter(name -> columnExists(table, name))
                .ifPresent(name -> work.run());
    }

    private boolean columnExists(String table, String column) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS"
                        + " WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class, table, column);
        // no answer counts as "not there"
        return Optional.ofNullable(count)
                .filter(found -> found > 0)
                .isPresent();
    }

    // the foreign key has to go before the column
    private void dropColumn(String table, String column) {
        ifColumnExists(table, column, () -> {
            foreignKeysOf(table, column).forEach(foreignKey ->
                    jdbcTemplate.execute("ALTER TABLE " + table + " DROP FOREIGN KEY " + foreignKey));
            jdbcTemplate.execute("ALTER TABLE " + table + " DROP COLUMN " + column);
        });
    }

    private List<String> foreignKeysOf(String table, String column) {
        return jdbcTemplate.queryForList(
                "SELECT CONSTRAINT_NAME FROM information_schema.KEY_COLUMN_USAGE"
                        + " WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?"
                        + " AND REFERENCED_TABLE_NAME IS NOT NULL",
                String.class, table, column);
    }
}
