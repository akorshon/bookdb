package com.bookdb.repository;

import com.bookdb.model.Book;
import com.bookdb.model.BookStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long> {

    Optional<Book> findByDetailUrl(String detailUrl);

    List<Book> findDistinctByAuthors_Id(Long authorId);

    Page<Book> findByStatus(BookStatus status, Pageable pageable);

    Page<Book> findByStatusAndTitleContainingIgnoreCase(BookStatus status, String title, Pageable pageable);

    Page<Book> findByStatusAndTitleSearchContainingIgnoreCase(BookStatus status, String titleSearch, Pageable pageable);

    Page<Book> findDistinctByStatusAndAuthors_NameContainingIgnoreCase(
            BookStatus status, String authorName, Pageable pageable);

    Page<Book> findDistinctByStatusAndAuthors_NameNativeContainingIgnoreCase(
            BookStatus status, String authorNameNative, Pageable pageable);

    List<Book> findBySubmittedBy_Id(Long userId);

    boolean existsByIdAndStatus(Long id, BookStatus status);
}
