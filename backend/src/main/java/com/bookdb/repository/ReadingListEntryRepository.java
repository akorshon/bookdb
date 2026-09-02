package com.bookdb.repository;

import com.bookdb.model.ReadingListEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReadingListEntryRepository extends JpaRepository<ReadingListEntry, Long> {

    Optional<ReadingListEntry> findByBook_IdAndUser_Id(Long bookId, Long userId);

    List<ReadingListEntry> findByUser_IdOrderByUpdatedAtDesc(Long userId);
}
