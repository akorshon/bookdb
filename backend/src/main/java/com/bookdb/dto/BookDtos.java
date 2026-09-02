package com.bookdb.dto;

import com.bookdb.model.Book;
import com.bookdb.model.Role;
import com.bookdb.model.User;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class BookDtos {

    private BookDtos() {
    }

    public record BookCreateRequest(
            @NotBlank @Size(max = 1000) String title,
            @Size(max = 200) String edition,
            @NotEmpty List<@NotBlank @Size(max = 500) String> authorNames,
            @Size(max = 4000) String summary,
            List<@NotBlank @Size(max = 200) String> categories) {
    }

    public record BookUpdateRequest(
            @NotBlank @Size(max = 1000) String title,
            @Size(max = 200) String edition,
            @NotEmpty List<@NotBlank @Size(max = 500) String> authorNames,
            @Size(max = 4000) String summary,
            List<@NotBlank @Size(max = 200) String> categories) {
    }

    public record RejectRequest(@NotBlank @Size(max = 1000) String reason) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record BookResponse(
            Long id,
            String title,
            String edition,
            String thumbnailUrl,
            String summary,
            String detailUrl,
            String source,
            String status,
            List<AuthorSummary> authors,
            List<CategorySummary> categories,
            Double averageRating,
            int ratingCount,
            Long submittedByUserId,
            String submittedByDisplayName,
            String rejectionReason,
            Instant createdAt,
            Instant updatedAt) {

        /** Full detail, including who submitted it - only for admin/owner contexts. */
        public static BookResponse from(Book b) {
            return new BookResponse(
                    b.getId(),
                    b.getTitle(),
                    b.getEdition(),
                    b.getThumbnailUrl(),
                    b.getSummary(),
                    b.getDetailUrl(),
                    b.getSource(),
                    b.getStatus().name(),
                    b.getAuthors().stream()
                            .map(AuthorSummary::from)
                            .sorted((a1, a2) -> a1.name().compareToIgnoreCase(a2.name()))
                            .toList(),
                    b.getCategories().stream()
                            .map(CategorySummary::from)
                            .sorted((a, c) -> a.name().compareToIgnoreCase(c.name()))
                            .toList(),
                    b.getAverageRating(),
                    b.getRatingCount(),
                    b.getSubmittedBy() != null ? b.getSubmittedBy().getId() : null,
                    b.getSubmittedBy() != null ? b.getSubmittedBy().getDisplayName() : null,
                    b.getRejectionReason(),
                    b.getCreatedAt(),
                    b.getUpdatedAt());
        }

        /** Published books are visible to everyone; drafts/pending/rejected only to their owner or an admin. */
        public boolean isVisibleTo(User caller) {
            if ("PUBLISHED".equals(status)) {
                return true;
            }
            boolean isSubmitter = caller != null && submittedByUserId != null
                    && submittedByUserId.equals(caller.getId());
            boolean isAdmin = caller != null && caller.getRole() == Role.ADMIN;
            return isSubmitter || isAdmin;
        }

        /**
         * For public/catalog contexts: internal/submission-tracking fields - who submitted a
         * book, its moderation status, rejection reason, and timestamps - are only included for
         * the submitter themselves or an admin (the audiences that actually need them, via "My
         * Books"/the admin queue). Everyone else (including anonymous visitors, e.g. the public
         * API) gets a trimmed response; {@code edition} and {@code detailUrl} (the scraped
         * source-site link - nothing in the UI links to it) are dropped for the same audience
         * too, as unused clutter in the public shape. Note {@code status} is safe to null out here even
         * though {@link #isVisibleTo} reads it - that check always runs on the pre-redaction
         * response, never on this trimmed copy.
         *
         * <p>This is a pure transform of an already-built {@code BookResponse}, deliberately
         * separate from {@link #from(Book)} - callers should build/fetch (and cache) the
         * caller-independent full response once, then apply this per-request. Caching the
         * *redacted* response instead would leak or hide submitter identity across users
         * sharing the same cache entry.
         */
        public BookResponse redactFor(User caller) {
            boolean isSubmitter = caller != null && submittedByUserId != null
                    && submittedByUserId.equals(caller.getId());
            boolean isAdmin = caller != null && caller.getRole() == Role.ADMIN;
            if (isSubmitter || isAdmin) {
                return this;
            }
            return new BookResponse(
                    id, title, null, thumbnailUrl, summary,
                    null, null, null, authors, categories,
                    averageRating, ratingCount,
                    null, null,
                    null, null, null);
        }
    }

    /**
     * What actually gets cached for {@code GET /api/books} - {@code Page<BookResponse>} isn't
     * safely round-trippable through Redis/Jackson (Spring itself warns PageImpl serialization
     * isn't guaranteed stable), so this concrete record is cached instead and rewrapped into a
     * {@code PageImpl} by the caller once read back.
     */
    public record CachedBookPage(List<BookResponse> content, long totalElements) {
    }
}
