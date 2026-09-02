package com.bookdb.service;

import com.bookdb.dto.ReviewDtos.ReviewRequest;
import com.bookdb.dto.ReviewDtos.ReviewResponse;
import com.bookdb.model.Book;
import com.bookdb.model.BookStatus;
import com.bookdb.model.Review;
import com.bookdb.model.Role;
import com.bookdb.model.User;
import com.bookdb.repository.BookRepository;
import com.bookdb.repository.ReviewRepository;
import com.bookdb.util.TextNormalizer;
import com.bookdb.web.ForbiddenException;
import com.bookdb.web.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookRepository bookRepository;
    private final BookService bookService;
    private final CacheManager cacheManager;

    /**
     * Caches {@link ReviewResponse} DTOs, not {@link Review} entities - Review's {@code book}/
     * {@code user} associations are lazy Hibernate proxies, which aren't safely serializable into
     * Redis. Mapping to the DTO here, while the transaction/session is still open, resolves them.
     */
    @Cacheable(value = "bookReviews", key = "#bookId")
    @Transactional(readOnly = true)
    public List<ReviewResponse> listForBook(Long bookId) {
        Book book = bookRepository.findById(bookId).orElseThrow(() -> new NotFoundException("Book not found"));
        if (book.getStatus() != BookStatus.PUBLISHED) {
            throw new NotFoundException("Book not found");
        }
        return reviewRepository.findByBook_IdOrderByCreatedAtDesc(bookId).stream()
                .map(ReviewResponse::from)
                .toList();
    }

    @CacheEvict(value = "bookReviews", key = "#bookId")
    @Transactional
    public Review upsert(Long bookId, ReviewRequest request, User caller) {
        Book book = bookRepository.findById(bookId).orElseThrow(() -> new NotFoundException("Book not found"));
        if (book.getStatus() != BookStatus.PUBLISHED) {
            throw new IllegalStateException("Only published books can be reviewed");
        }
        Review review = reviewRepository.findByBook_IdAndUser_Id(bookId, caller.getId()).orElseGet(Review::new);
        boolean isNew = review.getId() == null;
        review.setBook(book);
        review.setUser(caller);
        review.setRating(request.rating());
        review.setReviewText(TextNormalizer.normalize(request.reviewText()));
        Instant now = Instant.now();
        if (isNew) {
            review.setCreatedAt(now);
        }
        review.setUpdatedAt(now);
        review = reviewRepository.save(review);
        recomputeAggregate(book);
        return review;
    }

    @Transactional
    public void delete(Long reviewId, User caller) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Review not found"));
        boolean owner = review.getUser().getId().equals(caller.getId());
        boolean admin = caller.getRole() == Role.ADMIN;
        if (!owner && !admin) {
            throw new ForbiddenException("You do not own this review");
        }
        Book book = review.getBook();
        reviewRepository.delete(review);
        evictBookReviews(book.getId());
        recomputeAggregate(book);
    }

    private void evictBookReviews(Long bookId) {
        Cache bookReviews = cacheManager.getCache("bookReviews");
        if (bookReviews != null) {
            bookReviews.evict(bookId);
        }
    }

    private void recomputeAggregate(Book book) {
        List<Review> reviews = reviewRepository.findByBook_Id(book.getId());
        if (reviews.isEmpty()) {
            book.setAverageRating(null);
            book.setRatingCount(0);
        } else {
            double avg = reviews.stream().mapToInt(Review::getRating).average().orElse(0);
            book.setAverageRating(Math.round(avg * 10) / 10.0);
            book.setRatingCount(reviews.size());
        }
        bookService.evictForRatingChange(book);
    }
}
