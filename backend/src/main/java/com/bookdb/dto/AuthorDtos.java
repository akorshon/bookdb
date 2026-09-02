package com.bookdb.dto;

import com.bookdb.model.Author;
import com.bookdb.model.Role;
import com.bookdb.model.User;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class AuthorDtos {

    private AuthorDtos() {
    }

    public record AuthorCreateRequest(
            @NotBlank @Size(max = 500) String name,
            @Size(max = 500) String nameNative,
            @Size(max = 4000) String bio) {
    }

    public record AuthorUpdateRequest(
            @NotBlank @Size(max = 500) String name,
            @Size(max = 500) String nameNative,
            @Size(max = 4000) String bio) {
    }

    public record RejectRequest(@NotBlank @Size(max = 1000) String reason) {
    }

    /**
     * Richer superset of {@link AuthorSummary} for the submission/moderation-aware endpoints.
     * {@code AuthorSummary} itself stays the compact shape embedded in {@code BookResponse.authors()}
     * - which only ever contains PUBLISHED authors, so it never needs status/redaction.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AuthorResponse(
            Long id,
            String name,
            String nameNative,
            String slug,
            String imageUrl,
            String bio,
            String source,
            String status,
            Long submittedByUserId,
            String submittedByDisplayName,
            String rejectionReason,
            Instant createdAt,
            Instant updatedAt) {

        /** Full detail, including who submitted it - only for admin/owner contexts. */
        public static AuthorResponse from(Author a) {
            return new AuthorResponse(
                    a.getId(),
                    a.getName(),
                    a.getNameNative(),
                    a.getSlug(),
                    a.getImageUrl(),
                    a.getBio(),
                    a.getSource(),
                    a.getStatus().name(),
                    a.getSubmittedBy() != null ? a.getSubmittedBy().getId() : null,
                    a.getSubmittedBy() != null ? a.getSubmittedBy().getDisplayName() : null,
                    a.getRejectionReason(),
                    a.getCreatedAt(),
                    a.getUpdatedAt());
        }

        /** Published authors are visible to everyone; drafts/pending/rejected only to their owner or an admin. */
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
         * For public/catalog contexts: internal/submission-tracking fields - slug, moderation
         * status, who submitted it, rejection reason, and timestamps - are only included for the
         * submitter themselves or an admin (the audiences that actually need them, via "My
         * Authors"/the admin queue); same pattern as {@code BookResponse.redactFor}. Note
         * {@code status} is safe to null out here even though {@link #isVisibleTo} reads it -
         * that check always runs on the pre-redaction response, never on this trimmed copy.
         */
        public AuthorResponse redactFor(User caller) {
            boolean isSubmitter = caller != null && submittedByUserId != null
                    && submittedByUserId.equals(caller.getId());
            boolean isAdmin = caller != null && caller.getRole() == Role.ADMIN;
            if (isSubmitter || isAdmin) {
                return this;
            }
            return new AuthorResponse(
                    id, name, nameNative, null, imageUrl, bio, null, null,
                    null, null,
                    null, null, null);
        }
    }
}
