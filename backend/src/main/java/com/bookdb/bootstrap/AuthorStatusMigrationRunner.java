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
 * Safety net for the `authors.status` column added alongside author submission/approval
 * (see {@code BookStatusMigrationRunner}, the same pattern for {@code books.status}): the
 * column carries a DB-level DEFAULT 'PUBLISHED', so Hibernate's schema update already
 * backfills existing rows when it adds the column - this just guards against any row that
 * somehow still has a null status. Runs unconditionally at startup; a no-op once there's
 * nothing left to fix.
 */
@Component
@RequiredArgsConstructor
@Order(3)
public class AuthorStatusMigrationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AuthorStatusMigrationRunner.class);

    private final EntityManager entityManager;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int updated = entityManager.createQuery(
                        "update Author a set a.status = com.bookdb.model.AuthorStatus.PUBLISHED where a.status is null")
                .executeUpdate();
        if (updated > 0) {
            log.info("Migrated {} pre-existing author(s) to PUBLISHED status", updated);
        }
    }
}
