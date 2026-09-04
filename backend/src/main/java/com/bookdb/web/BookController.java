package com.bookdb.web;

import com.bookdb.dto.BookDtos.BookCreateRequest;
import com.bookdb.dto.BookDtos.BookResponse;
import com.bookdb.dto.BookDtos.BookUpdateRequest;
import com.bookdb.dto.BookDtos.CachedBookPage;
import com.bookdb.dto.BookDtos.CategorySection;
import com.bookdb.model.Book;
import com.bookdb.model.User;
import com.bookdb.service.BookService;
import com.bookdb.service.FileStorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;
    private final FileStorageService fileStorageService;

    @GetMapping("/books")
    public Page<BookResponse> list(@RequestParam(required = false) String query, Pageable pageable,
                                    @AuthenticationPrincipal User caller) {
        CachedBookPage cached = bookService.findPublishedResponses(query, pageable);
        List<BookResponse> redacted = cached.content().stream().map(r -> r.redactFor(caller)).toList();
        return new PageImpl<>(redacted, pageable, cached.totalElements());
    }

    /**
     * Browse page data: published books grouped into category sections, each capped to {@code size}
     * books (default 20, clamped to [1, 50]) instead of returning the whole catalog for the client
     * to group - see {@link BookService#findBrowseSections}.
     */
    @GetMapping("/books/sections")
    public List<CategorySection> sections(@RequestParam(defaultValue = "20") int size,
                                           @AuthenticationPrincipal User caller) {
        int clamped = Math.min(Math.max(size, 1), 50);
        return bookService.findBrowseSections(clamped).stream().map(s -> s.redactFor(caller)).toList();
    }

    @GetMapping("/books/{id}")
    public BookResponse get(@PathVariable Long id, @AuthenticationPrincipal User caller) {
        BookResponse full = bookService.findResponseById(id);
        if (full == null || !full.isVisibleTo(caller)) {
            throw new NotFoundException("Book not found");
        }
        return full.redactFor(caller);
    }

    @GetMapping("/my/books")
    public List<BookResponse> myBooks(@AuthenticationPrincipal User caller) {
        return bookService.findMine(caller).stream().map(BookResponse::from).toList();
    }

    @PostMapping("/books")
    @ResponseStatus(HttpStatus.CREATED)
    public BookResponse create(@Valid @RequestBody BookCreateRequest request, @AuthenticationPrincipal User caller) {
        return BookResponse.from(bookService.createDraft(request, caller));
    }

    @PutMapping("/books/{id}")
    public BookResponse update(@PathVariable Long id, @Valid @RequestBody BookUpdateRequest request,
                                @AuthenticationPrincipal User caller) {
        return BookResponse.from(bookService.update(id, request, caller));
    }

    @PostMapping("/books/{id}/submit")
    public BookResponse submit(@PathVariable Long id, @AuthenticationPrincipal User caller) {
        return BookResponse.from(bookService.submitForReview(id, caller));
    }

    @PostMapping("/books/{id}/cover")
    public BookResponse uploadCover(@PathVariable Long id, @RequestParam("file") MultipartFile file,
                                     @AuthenticationPrincipal User caller) {
        Book book = bookService.getVisible(id, caller);
        String url = fileStorageService.storeImage(file);
        bookService.setCoverUrl(book.getId(), url, caller);
        return BookResponse.from(bookService.getVisible(id, caller));
    }
}
