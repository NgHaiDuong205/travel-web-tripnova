package com.duong.travelweb;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class TravelwebApplicationTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void contextLoads() {
        try {
            jdbcTemplate.execute("ALTER TABLE room_type_amenities ADD COLUMN IF NOT EXISTS id SERIAL PRIMARY KEY;");
            System.out.println(">>> MIGRATION SUCCESSFUL: Added id column to room_type_amenities!");
        } catch (Exception e) {
            System.err.println(">>> MIGRATION ERROR: " + e.getMessage());
        }
    }
}
