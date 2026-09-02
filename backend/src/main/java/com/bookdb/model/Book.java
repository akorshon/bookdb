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
import jakarta.persistence.JoinTable;
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
@Table(name = "books")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 1000)
    private String title;

    /**
     * Lowercase, diacritic-free Latin phonetic key derived from {@link #title} (see
     * {@link com.bookdb.util.Transliteration}), so a Bengali title like "তোমাকে" is findable by
     * someone typing "tomake". Null for titles that are already Latin-script.
     */
    @Column(name = "title_search", length = 1000)
    private String titleSearch;

    @Column(length = 200)
    private String edition;

    @Column(name = "detail_url", length = 1000)
    private String detailUrl;

    @Column(name = "thumbnail_url", length = 1000)
    private String thumbnailUrl;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "book_authors",
            joinColumns = @JoinColumn(name = "book_id"),
            inverseJoinColumns = @JoinColumn(name = "author_id"))
    private Set<Author> authors = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "book_categories",
            joinColumns = @JoinColumn(name = "book_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id"))
    private Set<Category> categories = new HashSet<>();

    @Column(name = "scraped_at")
    private Instant scrapedAt;

    /** Which site this row was imported from (e.g. "rokomari.com", "boitoi.com.bd"). Null for
     * user-submitted books. Set by {@code ScrapeImportService}. */
    @Column(name = "source", length = 100)
    private String source;

    // Explicit DB-level DEFAULT so adding this NOT NULL column to the already-populated
    // table (230 scraped rows) succeeds under MySQL's strict mode.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED'")
    private BookStatus status = BookStatus.PUBLISHED;

    /** Null for system/scraper-imported books. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by")
    private User submittedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "average_rating")
    private Double averageRating;

    @Column(name = "rating_count", nullable = false, columnDefinition = "INT NOT NULL DEFAULT 0")
    private int ratingCount = 0;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
