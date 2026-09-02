package com.bookdb.dto;

import com.bookdb.model.Review;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class ReviewDtos {

    private ReviewDtos() {
    }

    public record ReviewRequest(@Min(1) @Max(5) int rating, @Size(max = 4000) String reviewText) {
    }

    public record ReviewResponse(
            Long id,
            Long bookId,
            Long userId,
            String userDisplayName,
            int rating,
            String reviewText,
            Instant createdAt,
            Instant updatedAt) {

        public static ReviewResponse from(Review r) {
            return new ReviewResponse(
                    r.getId(),
                    r.getBook().getId(),
                    r.getUser().getId(),
                    r.getUser().getDisplayName(),
                    r.getRating(),
                    r.getReviewText(),
                    r.getCreatedAt(),
                    r.getUpdatedAt());
        }
    }
}
