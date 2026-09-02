package com.bookdb.web;

import com.bookdb.dto.ReviewDtos.ReviewRequest;
import com.bookdb.dto.ReviewDtos.ReviewResponse;
import com.bookdb.model.User;
import com.bookdb.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/books/{bookId}/reviews")
    public List<ReviewResponse> list(@PathVariable Long bookId) {
        return reviewService.listForBook(bookId);
    }

    @PostMapping("/books/{bookId}/reviews")
    public ReviewResponse upsert(@PathVariable Long bookId, @Valid @RequestBody ReviewRequest request,
                                  @AuthenticationPrincipal User caller) {
        return ReviewResponse.from(reviewService.upsert(bookId, request, caller));
    }

    @DeleteMapping("/reviews/{id}")
    public void delete(@PathVariable Long id, @AuthenticationPrincipal User caller) {
        reviewService.delete(id, caller);
    }
}
