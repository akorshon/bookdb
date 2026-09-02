package com.bookdb.web;

import com.bookdb.dto.AuthorDtos.AuthorCreateRequest;
import com.bookdb.dto.AuthorDtos.AuthorResponse;
import com.bookdb.dto.AuthorDtos.AuthorUpdateRequest;
import com.bookdb.dto.BookDtos.BookResponse;
import com.bookdb.model.Author;
import com.bookdb.model.User;
import com.bookdb.service.AuthorService;
import com.bookdb.service.FileStorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthorController {

    private final AuthorService authorService;
    private final FileStorageService fileStorageService;

    @GetMapping("/authors")
    public List<AuthorResponse> list(@AuthenticationPrincipal User caller) {
        return authorService.listPublished().stream().map(a -> a.redactFor(caller)).toList();
    }

    @GetMapping("/authors/{id}")
    public AuthorResponse get(@PathVariable Long id, @AuthenticationPrincipal User caller) {
        AuthorResponse full = authorService.findResponseById(id);
        if (full == null || !full.isVisibleTo(caller)) {
            throw new NotFoundException("Author not found");
        }
        return full.redactFor(caller);
    }

    @GetMapping("/authors/{id}/books")
    public List<BookResponse> books(@PathVariable Long id, @AuthenticationPrincipal User caller) {
        return authorService.booksByAuthor(id).stream().map(b -> b.redactFor(caller)).toList();
    }

    @GetMapping("/my/authors")
    public List<AuthorResponse> myAuthors(@AuthenticationPrincipal User caller) {
        return authorService.findMine(caller);
    }

    @PostMapping("/authors")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthorResponse create(@Valid @RequestBody AuthorCreateRequest request, @AuthenticationPrincipal User caller) {
        return AuthorResponse.from(authorService.createDraft(request, caller));
    }

    @PutMapping("/authors/{id}")
    public AuthorResponse update(@PathVariable Long id, @Valid @RequestBody AuthorUpdateRequest request,
                                  @AuthenticationPrincipal User caller) {
        return AuthorResponse.from(authorService.update(id, request, caller));
    }

    @PostMapping("/authors/{id}/submit")
    public AuthorResponse submit(@PathVariable Long id, @AuthenticationPrincipal User caller) {
        return AuthorResponse.from(authorService.submitForReview(id, caller));
    }

    @PostMapping("/authors/{id}/photo")
    public AuthorResponse uploadPhoto(@PathVariable Long id, @RequestParam("file") MultipartFile file,
                                       @AuthenticationPrincipal User caller) {
        Author author = authorService.getVisible(id, caller);
        String url = fileStorageService.storeImage(file);
        authorService.setImageUrl(author.getId(), url, caller);
        return AuthorResponse.from(authorService.getVisible(id, caller));
    }
}
