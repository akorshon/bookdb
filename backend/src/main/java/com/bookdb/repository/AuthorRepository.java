package com.bookdb.repository;

import com.bookdb.model.Author;
import com.bookdb.model.AuthorStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AuthorRepository extends JpaRepository<Author, Long> {

    List<Author> findByNameContainingIgnoreCase(String name);

    Optional<Author> findByNameIgnoreCase(String name);

    /**
     * Used only by {@code BookService.resolveOrCreateAuthors}, so a book can't silently link to
     * a stranger's not-yet-approved draft author by name collision - the plain
     * {@link #findByNameIgnoreCase} above stays "any status" for the scraper/backfill tools that
     * legitimately want that.
     */
    Optional<Author> findByNameIgnoreCaseAndStatus(String name, AuthorStatus status);

    List<Author> findByStatus(AuthorStatus status);

    List<Author> findBySubmittedBy_Id(Long userId);

    /** Used by {@code ScrapeImportService} to upsert scraped authors, keyed on the source site's slug. */
    Optional<Author> findBySlug(String slug);
}
