package com.bookdb.bootstrap;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Safety net for the `books.status` column added alongside the draft/approval workflow: the
 * column carries a DB-level DEFAULT 'PUBLISHED' (see {@code Book.status}), so Hibernate's
 * schema update already backfills existing rows when it adds the column - this just guards
 * against any row that somehow still has a null status (e.g. schema managed by hand later).
 * Runs unconditionally at startup; a no-op once there's nothing left to fix.
 */
@Component
@RequiredArgsConstructor
@Order(2)
public class BookStatusMigrationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BookStatusMigrationRunner.class);

    private final EntityManager entityManager;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int updated = entityManager.createQuery(
                        "update Book b set b.status = com.bookdb.model.BookStatus.PUBLISHED where b.status is null")
                .executeUpdate();
        if (updated > 0) {
            log.info("Migrated {} pre-existing book(s) to PUBLISHED status", updated);
        }
    }
}
