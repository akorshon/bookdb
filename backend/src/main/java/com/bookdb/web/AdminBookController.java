package com.bookdb.web;

import com.bookdb.dto.BookDtos.BookResponse;
import com.bookdb.dto.BookDtos.RejectRequest;
import com.bookdb.model.BookStatus;
import com.bookdb.model.User;
import com.bookdb.service.BookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/books")
@RequiredArgsConstructor
public class AdminBookController {

    private final BookService bookService;

    @GetMapping
    public Page<BookResponse> list(@RequestParam(defaultValue = "PENDING_REVIEW") BookStatus status,
                                    Pageable pageable) {
        return bookService.findByStatus(status, pageable).map(BookResponse::from);
    }

    @PostMapping("/{id}/approve")
    public BookResponse approve(@PathVariable Long id, @AuthenticationPrincipal User admin) {
        return BookResponse.from(bookService.approve(id, admin));
    }

    @PostMapping("/{id}/reject")
    public BookResponse reject(@PathVariable Long id, @Valid @RequestBody RejectRequest request,
                                @AuthenticationPrincipal User admin) {
        return BookResponse.from(bookService.reject(id, request.reason(), admin));
    }
}
