package com.bookdb.service;

import com.bookdb.dto.ReadingListDtos.ReadingListEntryResponse;
import com.bookdb.model.Book;
import com.bookdb.model.ReadingListEntry;
import com.bookdb.model.ReadingStatus;
import com.bookdb.model.User;
import com.bookdb.repository.ReadingListEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReadingListService {

    private final ReadingListEntryRepository readingListEntryRepository;
    private final BookService bookService;

    public Optional<ReadingListEntry> getMyStatus(Long bookId, User caller) {
        return readingListEntryRepository.findByBook_IdAndUser_Id(bookId, caller.getId());
    }

    @Transactional
    public ReadingListEntry setStatus(Long bookId, ReadingStatus status, User caller) {
        // Reuses the existing visibility rule: you can't shelve a book you're not allowed to see.
        Book book = bookService.getVisible(bookId, caller);
        ReadingListEntry entry = readingListEntryRepository.findByBook_IdAndUser_Id(bookId, caller.getId())
                .orElseGet(ReadingListEntry::new);
        boolean isNew = entry.getId() == null;
        entry.setBook(book);
        entry.setUser(caller);
        entry.setStatus(status);
        Instant now = Instant.now();
        if (isNew) {
            entry.setCreatedAt(now);
        }
        entry.setUpdatedAt(now);
        return readingListEntryRepository.save(entry);
    }

    @Transactional
    public void remove(Long bookId, User caller) {
        readingListEntryRepository.findByBook_IdAndUser_Id(bookId, caller.getId())
                .ifPresent(readingListEntryRepository::delete);
    }

    /**
     * Maps to {@link ReadingListEntryResponse} here (not in the controller) - each entry's
     * {@code book} association is lazy, so building the DTO (which reads the book's authors/
     * categories) needs to happen while the transaction/session is still open.
     */
    @Transactional(readOnly = true)
    public List<ReadingListEntryResponse> findMine(User caller) {
        return readingListEntryRepository.findByUser_IdOrderByUpdatedAtDesc(caller.getId()).stream()
                .map(e -> ReadingListEntryResponse.from(e, caller))
                .toList();
    }
}
