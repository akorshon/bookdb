package com.bookdb.dto;

import com.bookdb.dto.BookDtos.BookResponse;
import com.bookdb.model.ReadingListEntry;
import com.bookdb.model.ReadingStatus;
import com.bookdb.model.User;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public final class ReadingListDtos {

    private ReadingListDtos() {
    }

    public record ReadingStatusRequest(@NotNull ReadingStatus status) {
    }

    /** For the single-book get/set/remove endpoints - {@code status} is null when not on the list. */
    public record ReadingStatusResponse(ReadingStatus status, Instant updatedAt) {

        public static final ReadingStatusResponse EMPTY = new ReadingStatusResponse(null, null);

        public static ReadingStatusResponse from(ReadingListEntry e) {
            return new ReadingStatusResponse(e.getStatus(), e.getUpdatedAt());
        }
    }

    /** For "my reading list" - embeds the full book so the frontend can render a book card directly. */
    public record ReadingListEntryResponse(
            Long id, BookResponse book, ReadingStatus status, Instant createdAt, Instant updatedAt) {

        public static ReadingListEntryResponse from(ReadingListEntry e, User caller) {
            return new ReadingListEntryResponse(
                    e.getId(),
                    BookResponse.from(e.getBook()).redactFor(caller),
                    e.getStatus(),
                    e.getCreatedAt(),
                    e.getUpdatedAt());
        }
    }
}
