package com.bookdb.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@Table(name = "authors")
public class Author {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String name;

    /**
     * The author's name in its native script (typically Bengali), captured from the source
     * site rather than algorithmically guessed - generic Latin-to-Bengali transliteration of a
     * name like "Humayun Ahmed" doesn't reliably reproduce real Bengali spelling. Null for
     * authors where only one script's name is known (e.g. most user-submitted authors).
     */
    @Column(name = "name_native", length = 500)
    private String nameNative;

    @Column(length = 500)
    private String slug;

    @Column(name = "profile_url", length = 1000)
    private String profileUrl;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Column(name = "scraped_at")
    private Instant scrapedAt;

    /** Which site this row was imported from (e.g. "rokomari.com", "boitoi.com.bd"). Null for
     * user-submitted authors. Set by {@code ScrapeImportService}. */
    @Column(name = "source", length = 100)
    private String source;

    // Explicit DB-level DEFAULT so adding this NOT NULL column to the already-populated
    // table succeeds under MySQL's strict mode - same trick as Book.status.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED'")
    private AuthorStatus status = AuthorStatus.PUBLISHED;

    /** Null for system/scraper-imported or auto-created-via-book authors. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by")
    private User submittedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @ManyToMany(mappedBy = "authors")
    private Set<Book> books = new HashSet<>();
}
