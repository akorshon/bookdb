package com.bookdb.web;

import com.bookdb.dto.AuthorDtos.AuthorResponse;
import com.bookdb.dto.AuthorDtos.RejectRequest;
import com.bookdb.model.AuthorStatus;
import com.bookdb.model.User;
import com.bookdb.service.AuthorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/authors")
@RequiredArgsConstructor
public class AdminAuthorController {

    private final AuthorService authorService;

    @GetMapping
    public List<AuthorResponse> list(@RequestParam(defaultValue = "PENDING_REVIEW") AuthorStatus status) {
        return authorService.findByStatus(status).stream().map(AuthorResponse::from).toList();
    }

    @PostMapping("/{id}/approve")
    public AuthorResponse approve(@PathVariable Long id, @AuthenticationPrincipal User admin) {
        return AuthorResponse.from(authorService.approve(id, admin));
    }

    @PostMapping("/{id}/reject")
    public AuthorResponse reject(@PathVariable Long id, @Valid @RequestBody RejectRequest request,
                                  @AuthenticationPrincipal User admin) {
        return AuthorResponse.from(authorService.reject(id, request.reason(), admin));
    }
}
