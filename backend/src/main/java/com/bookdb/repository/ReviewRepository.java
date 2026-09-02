package com.bookdb.repository;

import com.bookdb.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByBook_IdOrderByCreatedAtDesc(Long bookId);

    Optional<Review> findByBook_IdAndUser_Id(Long bookId, Long userId);

    List<Review> findByBook_Id(Long bookId);
}
