package com.bookdb.service;

import com.bookdb.dto.AuthorDtos.AuthorCreateRequest;
import com.bookdb.dto.AuthorDtos.AuthorResponse;
import com.bookdb.dto.AuthorDtos.AuthorUpdateRequest;
import com.bookdb.dto.BookDtos.BookResponse;
import com.bookdb.model.Author;
import com.bookdb.model.AuthorStatus;
import com.bookdb.model.Book;
import com.bookdb.model.BookStatus;
import com.bookdb.model.Role;
import com.bookdb.model.User;
import com.bookdb.repository.AuthorRepository;
import com.bookdb.repository.BookRepository;
import com.bookdb.util.TextNormalizer;
import com.bookdb.web.ForbiddenException;
import com.bookdb.web.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

/**
 * Read-only cached lookups + the author submission/moderation flow, mirroring
 * {@code BookService}. An author is normally only editable while DRAFT/REJECTED, which
 * (combined with {@code BookService.resolveOrCreateAuthors} only linking PUBLISHED authors
 * to books) would mean a PUBLISHED author's data can't change after being embedded in a
 * cached {@code BookResponse}/{@code bookLists}/{@code authorBooks}/{@code authors} entry -
 * except an admin can also edit an already-PUBLISHED author (see {@link #update}), so those
 * caches do need eviction in that case.
 */
@Service
@RequiredArgsConstructor
public class AuthorService {

    private final AuthorRepository authorRepository;
    private final BookRepository bookRepository;
    private final CacheManager cacheManager;

    @Cacheable("authors")
    @Transactional(readOnly = true)
    public List<AuthorResponse> listPublished() {
        return authorRepository.findByStatus(AuthorStatus.PUBLISHED).stream()
                .map(AuthorResponse::from)
                .toList();
    }

    @Cacheable(value = "authorById", key = "#id", unless = "#result == null")
    @Transactional(readOnly = true)
    public AuthorResponse findResponseById(Long id) {
        return authorRepository.findById(id).map(AuthorResponse::from).orElse(null);
    }

    @Cacheable(value = "authorBooks", key = "#id")
    @Transactional(readOnly = true)
    public List<BookResponse> booksByAuthor(Long id) {
        return bookRepository.findDistinctByAuthors_Id(id).stream()
                .filter(b -> b.getStatus() == BookStatus.PUBLISHED)
                .map(BookResponse::from)
                .toList();
    }

    public List<AuthorResponse> findMine(User caller) {
        return authorRepository.findBySubmittedBy_Id(caller.getId()).stream()
                .map(AuthorResponse::from)
                .toList();
    }

    /** Visible if published, or the caller is the submitter/an admin. Entity-returning, for mutation flows. */
    public Author getVisible(Long id, User caller) {
        Author author = authorRepository.findById(id).orElseThrow(() -> new NotFoundException("Author not found"));
        if (author.getStatus() == AuthorStatus.PUBLISHED) {
            return author;
        }
        if (caller != null && (isAdmin(caller) || isOwner(author, caller))) {
            return author;
        }
        throw new NotFoundException("Author not found");
    }

    public List<Author> findByStatus(AuthorStatus status) {
        return authorRepository.findByStatus(status);
    }

    @CacheEvict(value = "authors", allEntries = true)
    @Transactional
    public Author createDraft(AuthorCreateRequest request, User caller) {
        Author author = new Author();
        applyFields(author, request.name(), request.nameNative(), request.bio());
        author.setStatus(AuthorStatus.DRAFT);
        author.setSubmittedBy(caller);
        author.setCreatedAt(Instant.now());
        author.setUpdatedAt(Instant.now());
        return authorRepository.save(author);
    }

    @CacheEvict(value = "authorById", key = "#id")
    @Transactional
    public Author update(Long id, AuthorUpdateRequest request, User caller) {
        Author author = authorRepository.findById(id).orElseThrow(() -> new NotFoundException("Author not found"));
        // Admins can edit any author (including published ones) - e.g. to fix a bio. A regular
        // submitter is restricted to their own still-unpublished (draft/rejected) authors.
        if (!isAdmin(caller)) {
            requireOwner(author, caller);
            requireEditable(author);
        }
        applyFields(author, request.name(), request.nameNative(), request.bio());
        author.setUpdatedAt(Instant.now());
        if (author.getStatus() == AuthorStatus.PUBLISHED) {
            // Only reachable here for an admin edit (a submitter's own authors are always
            // draft/rejected above) - the published author's name/nameNative/slug/imageUrl may
            // already be embedded (via AuthorSummary) in cached bookLists/bookById/authorBooks
            // entries for their books, and the author itself in the "authors" list cache.
            evictAuthorsList();
            evictBookCachesFor(author);
        }
        return author;
    }

    @CacheEvict(value = "authorById", key = "#id")
    @Transactional
    public Author submitForReview(Long id, User caller) {
        Author author = authorRepository.findById(id).orElseThrow(() -> new NotFoundException("Author not found"));
        requireOwner(author, caller);
        requireEditable(author);
        author.setStatus(AuthorStatus.PENDING_REVIEW);
        author.setRejectionReason(null);
        author.setUpdatedAt(Instant.now());
        return author;
    }

    @CacheEvict(value = "authorById", key = "#id")
    @Transactional
    public void setImageUrl(Long id, String url, User caller) {
        Author author = authorRepository.findById(id).orElseThrow(() -> new NotFoundException("Author not found"));
        requireOwner(author, caller);
        requireEditable(author);
        author.setImageUrl(url);
        author.setUpdatedAt(Instant.now());
    }

    @Caching(evict = {
            @CacheEvict(value = "authors", allEntries = true),
            @CacheEvict(value = "authorById", key = "#id")
    })
    @Transactional
    public Author approve(Long id, User admin) {
        Author author = authorRepository.findById(id).orElseThrow(() -> new NotFoundException("Author not found"));
        if (author.getStatus() != AuthorStatus.PENDING_REVIEW) {
            throw new IllegalStateException("Only authors pending review can be approved");
        }
        author.setStatus(AuthorStatus.PUBLISHED);
        author.setApprovedBy(admin);
        author.setRejectionReason(null);
        author.setUpdatedAt(Instant.now());
        return author;
    }

    @CacheEvict(value = "authorById", key = "#id")
    @Transactional
    public Author reject(Long id, String reason, User admin) {
        Author author = authorRepository.findById(id).orElseThrow(() -> new NotFoundException("Author not found"));
        if (author.getStatus() != AuthorStatus.PENDING_REVIEW) {
            throw new IllegalStateException("Only authors pending review can be rejected");
        }
        // Reject is only reachable from PENDING_REVIEW, i.e. this author was never PUBLISHED and
        // so was never in the public `authors` cache - nothing to evict there.
        author.setStatus(AuthorStatus.REJECTED);
        author.setApprovedBy(admin);
        author.setRejectionReason(reason);
        author.setUpdatedAt(Instant.now());
        return author;
    }

    /** Admin-only. Blocked if any book still references this author (see {@code AdminAuthorController}). */
    @CacheEvict(value = "authors", allEntries = true)
    @Transactional
    public void delete(Long id) {
        Author author = authorRepository.findById(id).orElseThrow(() -> new NotFoundException("Author not found"));
        if (bookRepository.existsByAuthors_Id(id)) {
            throw new IllegalStateException("Cannot delete an author who still has books");
        }
        authorRepository.delete(author);
        Cache authorById = cacheManager.getCache("authorById");
        if (authorById != null) {
            authorById.evict(id);
        }
    }

    private void evictAuthorsList() {
        Cache authors = cacheManager.getCache("authors");
        if (authors != null) {
            authors.clear();
        }
    }

    private void evictBookCachesFor(Author author) {
        Cache bookLists = cacheManager.getCache("bookLists");
        if (bookLists != null) {
            bookLists.clear();
        }
        Cache authorBooks = cacheManager.getCache("authorBooks");
        if (authorBooks != null) {
            authorBooks.evict(author.getId());
        }
        Cache bookById = cacheManager.getCache("bookById");
        if (bookById != null) {
            for (Book book : bookRepository.findDistinctByAuthors_Id(author.getId())) {
                bookById.evict(book.getId());
            }
        }
    }

    private void applyFields(Author author, String name, String nameNative, String bio) {
        String normalizedName = TextNormalizer.normalize(name.trim());
        author.setName(normalizedName);
        author.setNameNative(StringUtils.hasText(nameNative) ? TextNormalizer.normalize(nameNative.trim()) : null);
        author.setBio(TextNormalizer.normalize(bio));
        if (author.getSlug() == null) {
            author.setSlug(normalizedName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-"));
        }
    }

    private boolean isOwner(Author author, User user) {
        return author.getSubmittedBy() != null && author.getSubmittedBy().getId().equals(user.getId());
    }

    private boolean isAdmin(User user) {
        return user.getRole() == Role.ADMIN;
    }

    private void requireOwner(Author author, User user) {
        if (!isOwner(author, user)) {
            throw new ForbiddenException("You do not own this author");
        }
    }

    private void requireEditable(Author author) {
        if (author.getStatus() != AuthorStatus.DRAFT && author.getStatus() != AuthorStatus.REJECTED) {
            throw new IllegalStateException("Only draft or rejected authors can be edited");
        }
    }
}
