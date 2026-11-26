package com.travel.booking_service.config;

import java.sql.Statement;

import javax.sql.DataSource;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class BookingStatusConstraintMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public BookingStatusConstraintMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            if (!"PostgreSQL".equals(connection.getMetaData().getDatabaseProductName())) {
                return null;
            }

            try (Statement statement = connection.createStatement()) {
                statement.execute("ALTER TABLE bookings DROP CONSTRAINT IF EXISTS bookings_status_check");
                statement.execute("""
                        ALTER TABLE bookings
                        ADD CONSTRAINT bookings_status_check
                        CHECK (status IN ('PENDING_PAYMENT', 'CONFIRMED', 'CANCELLED'))
                        """);
            }
            return null;
        });
    }
}
