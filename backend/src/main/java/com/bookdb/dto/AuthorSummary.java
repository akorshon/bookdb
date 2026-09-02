package com.bookdb.dto;

import com.bookdb.model.Author;

/**
 * Compact shape embedded in {@code BookResponse.authors()} - deliberately excludes {@code bio}
 * (long-form text with no use in a book listing; the full profile is one call away via
 * {@code GET /api/authors/{id}}, which returns the richer {@code AuthorDtos.AuthorResponse}).
 */
public record AuthorSummary(
        Long id,
        String name,
        String nameNative,
        String slug,
        String imageUrl) {

    public static AuthorSummary from(Author a) {
        if (a == null) {
            return null;
        }
        return new AuthorSummary(a.getId(), a.getName(), a.getNameNative(), a.getSlug(), a.getImageUrl());
    }
}
