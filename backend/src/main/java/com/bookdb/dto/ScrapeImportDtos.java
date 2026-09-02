package com.bookdb.dto;

import java.time.Instant;
import java.util.List;

/**
 * Mirrors the JSON shape written by the standalone {@code bookdbscraper} project
 * ({@code com.bookdbscraper.model.ScrapeExport} et al.) - kept as an independent, intentionally
 * duplicated copy here since bookdb and bookdbscraper are separate deployable projects with no
 * shared library between them.
 */
public final class ScrapeImportDtos {

    private ScrapeImportDtos() {
    }

    public record ScrapeExport(String source, Instant scrapedAt, List<ScrapedAuthor> authors,
                                List<ScrapedBook> books) {
    }

    public record ScrapedAuthor(String source, String slug, String name, String nameNative, String profileUrl,
                                 String imageUrl, String bio) {
    }

    public record ScrapedBook(String source, String detailUrl, String title, String edition, String thumbnailUrl,
                               String summary, List<String> categories, List<String> authorSlugs) {
    }

    public record ScrapeImportSummary(int authorsCreated, int authorsUpdated, int booksCreated, int booksUpdated,
                                       List<String> errors) {
    }
}
