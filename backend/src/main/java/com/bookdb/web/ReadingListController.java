package com.bookdb.web;

import com.bookdb.dto.ReadingListDtos.ReadingListEntryResponse;
import com.bookdb.dto.ReadingListDtos.ReadingStatusRequest;
import com.bookdb.dto.ReadingListDtos.ReadingStatusResponse;
import com.bookdb.model.User;
import com.bookdb.service.ReadingListService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReadingListController {

    private final ReadingListService readingListService;

    @GetMapping("/books/{bookId}/reading-status")
    public ReadingStatusResponse get(@PathVariable Long bookId, @AuthenticationPrincipal User caller) {
        return readingListService.getMyStatus(bookId, caller)
                .map(ReadingStatusResponse::from)
                .orElse(ReadingStatusResponse.EMPTY);
    }

    @PutMapping("/books/{bookId}/reading-status")
    public ReadingStatusResponse set(@PathVariable Long bookId, @Valid @RequestBody ReadingStatusRequest request,
                                      @AuthenticationPrincipal User caller) {
        return ReadingStatusResponse.from(readingListService.setStatus(bookId, request.status(), caller));
    }

    @DeleteMapping("/books/{bookId}/reading-status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable Long bookId, @AuthenticationPrincipal User caller) {
        readingListService.remove(bookId, caller);
    }

    @GetMapping("/my/reading-list")
    public List<ReadingListEntryResponse> mine(@AuthenticationPrincipal User caller) {
        return readingListService.findMine(caller);
    }
}
