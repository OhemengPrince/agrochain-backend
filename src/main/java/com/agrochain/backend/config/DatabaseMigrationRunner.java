package com.agrochain.backend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

// ddl-auto=update was not picking up ChatMessage.deleted on this database —
// confirmed via information_schema that the column was genuinely absent
// despite the entity/mapping being correct and the app restarting cleanly.
// ADD COLUMN IF NOT EXISTS makes this idempotent and safe to run on every
// startup rather than only once.
@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseMigrationRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute(
                    "ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT false"
            );
            log.info("[Migration] chat_messages.deleted column present (added if missing)");
        } catch (Exception e) {
            log.error("[Migration] Failed to ensure chat_messages.deleted column", e);
        }
    }
}
