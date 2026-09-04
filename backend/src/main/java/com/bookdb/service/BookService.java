package com.bookdb.service;

import com.bookdb.dto.BookDtos.BookCreateRequest;
import com.bookdb.dto.BookDtos.BookResponse;
import com.bookdb.dto.BookDtos.BookUpdateRequest;
import com.bookdb.dto.BookDtos.CachedBookPage;
import com.bookdb.dto.BookDtos.CategorySection;
import com.bookdb.dto.CategorySummary;
import com.bookdb.model.Author;
import com.bookdb.model.Book;
import com.bookdb.model.BookStatus;
import com.bookdb.model.User;
import com.bookdb.repository.AuthorRepository;
import com.bookdb.repository.BookRepository;
import com.bookdb.repository.ReadingListEntryRepository;
import com.bookdb.repository.ReviewRepository;
import com.bookdb.util.TextNormalizer;
import com.bookdb.web.ForbiddenException;
import com.bookdb.web.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final CategoryService categoryService;
    private final ReviewRepository reviewRepository;
    private final ReadingListEntryRepository readingListEntryRepository;
    private final CacheManager cacheManager;

    /**
     * Searches title, title's native-script variant, author name, and author's native-script
     * name, in that order (primary/English-ish fields first, native second), returning the first
     * tier with any results - so a query matches regardless of which field the match lives in.
     */
    public Page<Book> findPublished(String query, Pageable pageable) {
        if (!StringUtils.hasText(query)) {
            return bookRepository.findByStatus(BookStatus.PUBLISHED, pageable);
        }
        String q = TextNormalizer.normalize(query.trim());

        Page<Book> byTitle = bookRepository.findByStatusAndTitleContainingIgnoreCase(BookStatus.PUBLISHED, q, pageable);
        if (byTitle.hasContent()) {
            return byTitle;
        }
        Page<Book> byTitleNative =
                bookRepository.findByStatusAndTitleNativeContainingIgnoreCase(BookStatus.PUBLISHED, q, pageable);
        if (byTitleNative.hasContent()) {
            return byTitleNative;
        }
        Page<Book> byAuthorName =
                bookRepository.findDistinctByStatusAndAuthors_NameContainingIgnoreCase(BookStatus.PUBLISHED, q, pageable);
        if (byAuthorName.hasContent()) {
            return byAuthorName;
        }
        return bookRepository.findDistinctByStatusAndAuthors_NameNativeContainingIgnoreCase(
                BookStatus.PUBLISHED, q, pageable);
    }

    /**
     * Cached, DTO-returning counterpart of {@link #findPublished}, used by the public list
     * endpoint. Caches a caller-independent {@link CachedBookPage} (full/unredacted responses) -
     * per-caller redaction is applied by the controller on every read, not baked into the cache
     * entry, so one cached page can't leak or hide a submitter's identity across different callers.
     */
    @Cacheable(value = "bookLists",
            key = "(#query != null ? #query : '') + '|' + #pageable.pageNumber + '|' + #pageable.pageSize + '|' + #pageable.sort.toString()")
    @Transactional(readOnly = true)
    public CachedBookPage findPublishedResponses(String query, Pageable pageable) {
        Page<Book> page = findPublished(query, pageable);
        List<BookResponse> content = page.getContent().stream().map(BookResponse::from).toList();
        return new CachedBookPage(content, page.getTotalElements());
    }

    /**
     * Cached, DTO-returning data for the browse page: one row per category that has at least one
     * published book (most-populous first, "Uncategorized" last), each capped to
     * {@code booksPerSection} books - not the full catalog fetched-and-grouped client-side. Keeps
     * the browse page's payload and image count bounded regardless of how large the catalog
     * grows, instead of shipping every published book (and rendering every cover) on every visit.
     * Cached the same way as {@link #findPublishedResponses}: caller-independent, redacted by the
     * controller per request.
     */
    @Cacheable(value = "bookLists", key = "'sections|' + #booksPerSection")
    @Transactional(readOnly = true)
    public List<CategorySection> findBrowseSections(int booksPerSection) {
        Pageable firstPage = PageRequest.of(0, booksPerSection);
        List<CategorySection> sections = new ArrayList<>();
        for (CategorySummary category : categoryService.listAll()) {
            long total = bookRepository.countByStatusAndCategories_Id(BookStatus.PUBLISHED, category.id());
            if (total == 0) {
                continue;
            }
            List<BookResponse> books = bookRepository
                    .findByStatusAndCategories_Id(BookStatus.PUBLISHED, category.id(), firstPage)
                    .getContent().stream().map(BookResponse::from).toList();
            sections.add(new CategorySection(category.name(), total, books));
        }
        sections.sort((a, b) -> Long.compare(b.totalElements(), a.totalElements()));

        // Uncategorized always goes last, regardless of size - it's a catch-all bucket, not a
        // real category, so it shouldn't compete with real categories for top billing.
        long uncategorizedTotal = bookRepository.countByStatusAndCategoriesIsEmpty(BookStatus.PUBLISHED);
        if (uncategorizedTotal > 0) {
            List<BookResponse> books = bookRepository.findByStatusAndCategoriesIsEmpty(BookStatus.PUBLISHED, firstPage)
                    .getContent().stream().map(BookResponse::from).toList();
            sections.add(new CategorySection("Uncategorized", uncategorizedTotal, books));
        }
        return sections;
    }

    /**
     * Cached full (unredacted) response for a single book, regardless of status - callers (see
     * {@code BookController.get}) run their visibility check against this cached DTO via
     * {@code BookResponse.isVisibleTo} rather than hitting the DB, since it only needs
     * {@code status}/{@code submittedByUserId}, both already present here.
     * Returns {@code null} for a missing id (not cached, since {@code cache-null-values=false}).
     */
    @Cacheable(value = "bookById", key = "#id", unless = "#result == null")
    @Transactional(readOnly = true)
    public BookResponse findResponseById(Long id) {
        return bookRepository.findById(id).map(BookResponse::from).orElse(null);
    }

    /** Visible if published, or the caller is the submitter/an admin. */
    public Book getVisible(Long id, User caller) {
        Book book = bookRepository.findById(id).orElseThrow(() -> new NotFoundException("Book not found"));
        if (book.getStatus() == BookStatus.PUBLISHED) {
            return book;
        }
        if (caller != null && (isAdmin(caller) || isOwner(book, caller))) {
            return book;
        }
        throw new NotFoundException("Book not found");
    }

    public List<Book> findMine(User caller) {
        return bookRepository.findBySubmittedBy_Id(caller.getId());
    }

    @Caching(evict = {
            @CacheEvict(value = "authors", allEntries = true),
            @CacheEvict(value = "categories", allEntries = true)
    })
    @Transactional
    public Book createDraft(BookCreateRequest request, User caller) {
        Book book = new Book();
        String title = TextNormalizer.normalize(request.title().trim());
        book.setTitle(title);
        book.setTitleNative(StringUtils.hasText(request.titleNative())
                ? TextNormalizer.normalize(request.titleNative().trim()) : null);
        book.setEdition(TextNormalizer.normalize(request.edition()));
        book.setSummary(TextNormalizer.normalize(request.summary()));
        book.setAuthors(resolveOrCreateAuthors(request.authorNames()));
        book.setCategories(categoryService.findOrCreateByNames(request.categories()));
        book.setStatus(BookStatus.DRAFT);
        book.setSubmittedBy(caller);
        book.setCreatedAt(Instant.now());
        book.setUpdatedAt(Instant.now());
        return bookRepository.save(book);
    }

    @Caching(evict = {
            @CacheEvict(value = "authors", allEntries = true),
            @CacheEvict(value = "categories", allEntries = true),
            @CacheEvict(value = "bookById", key = "#id")
    })
    @Transactional
    public Book update(Long id, BookUpdateRequest request, User caller) {
        Book book = bookRepository.findById(id).orElseThrow(() -> new NotFoundException("Book not found"));
        // Admins can edit any book (including published ones) - e.g. to fix a category. A
        // regular submitter is restricted to their own still-unpublished (draft/rejected) books.
        if (!isAdmin(caller)) {
            requireOwner(book, caller);
            if (book.getStatus() != BookStatus.DRAFT && book.getStatus() != BookStatus.REJECTED) {
                throw new IllegalStateException("Only draft or rejected books can be edited");
            }
        }
        Set<Author> previousAuthors = book.getAuthors();
        String title = TextNormalizer.normalize(request.title().trim());
        book.setTitle(title);
        book.setTitleNative(StringUtils.hasText(request.titleNative())
                ? TextNormalizer.normalize(request.titleNative().trim()) : null);
        book.setEdition(TextNormalizer.normalize(request.edition()));
        book.setSummary(TextNormalizer.normalize(request.summary()));
        book.setAuthors(resolveOrCreateAuthors(request.authorNames()));
        book.setCategories(categoryService.findOrCreateByNames(request.categories()));
        book.setUpdatedAt(Instant.now());
        if (book.getStatus() == BookStatus.PUBLISHED) {
            // Only reachable here for an admin edit (a submitter's own books are always
            // draft/rejected above) - the published book may already be in cached listings and
            // on its authors' pages, both keyed off the pre-edit data.
            evictBookLists();
            Set<Author> affectedAuthors = new HashSet<>(previousAuthors);
            affectedAuthors.addAll(book.getAuthors());
            evictAuthorBooks(affectedAuthors);
        }
        return book;
    }

    @CacheEvict(value = "bookById", key = "#id")
    @Transactional
    public Book submitForReview(Long id, User caller) {
        Book book = bookRepository.findById(id).orElseThrow(() -> new NotFoundException("Book not found"));
        requireOwner(book, caller);
        if (book.getStatus() != BookStatus.DRAFT && book.getStatus() != BookStatus.REJECTED) {
            throw new IllegalStateException("Only draft or rejected books can be submitted for review");
        }
        book.setStatus(BookStatus.PENDING_REVIEW);
        book.setRejectionReason(null);
        book.setUpdatedAt(Instant.now());
        return book;
    }

    /**
     * Admin-only. Reviews and reading-list entries have a NOT NULL, uncascaded {@code book_id} FK
     * and are meaningless without the book, so they're deleted explicitly before the book itself.
     */
    @Transactional
    public void delete(Long id) {
        Book book = bookRepository.findById(id).orElseThrow(() -> new NotFoundException("Book not found"));
        boolean wasPublished = book.getStatus() == BookStatus.PUBLISHED;
        Set<Author> authors = new HashSet<>(book.getAuthors());
        reviewRepository.deleteByBook_Id(id);
        readingListEntryRepository.deleteByBook_Id(id);
        bookRepository.delete(book);
        Cache bookById = cacheManager.getCache("bookById");
        if (bookById != null) {
            bookById.evict(id);
        }
        if (wasPublished) {
            evictBookLists();
            evictAuthorBooks(authors);
        }
    }

    public Page<Book> findByStatus(BookStatus status, Pageable pageable) {
        return bookRepository.findByStatus(status, pageable);
    }

    @CacheEvict(value = "bookById", key = "#id")
    @Transactional
    public Book approve(Long id, User admin) {
        Book book = bookRepository.findById(id).orElseThrow(() -> new NotFoundException("Book not found"));
        if (book.getStatus() != BookStatus.PENDING_REVIEW) {
            throw new IllegalStateException("Only books pending review can be approved");
        }
        book.setStatus(BookStatus.PUBLISHED);
        book.setApprovedBy(admin);
        book.setRejectionReason(null);
        book.setUpdatedAt(Instant.now());
        // The book is now publicly visible: any cached listing page might now include it, and it
        // belongs on each of its authors' book pages.
        evictBookLists();
        evictAuthorBooks(book.getAuthors());
        return book;
    }

    @CacheEvict(value = "bookById", key = "#id")
    @Transactional
    public Book reject(Long id, String reason, User admin) {
        Book book = bookRepository.findById(id).orElseThrow(() -> new NotFoundException("Book not found"));
        if (book.getStatus() != BookStatus.PENDING_REVIEW) {
            throw new IllegalStateException("Only books pending review can be rejected");
        }
        // Reject is only reachable from PENDING_REVIEW, i.e. this book was never PUBLISHED and so
        // was never in bookLists/authorBooks - nothing to evict there.
        book.setStatus(BookStatus.REJECTED);
        book.setApprovedBy(admin);
        book.setRejectionReason(reason);
        book.setUpdatedAt(Instant.now());
        return book;
    }

    @CacheEvict(value = "bookById", key = "#id")
    @Transactional
    public void setCoverUrl(Long id, String url, User caller) {
        Book book = bookRepository.findById(id).orElseThrow(() -> new NotFoundException("Book not found"));
        requireOwner(book, caller);
        book.setThumbnailUrl(url);
        book.setUpdatedAt(Instant.now());
        // Cover shows on every card that renders this book - if it's published, that includes
        // list/search pages and each of its authors' book pages.
        if (book.getStatus() == BookStatus.PUBLISHED) {
            evictBookLists();
            evictAuthorBooks(book.getAuthors());
        }
    }

    /** Evicted here (not via ReviewService) since Book is the entity/cache that owns the aggregate. */
    public void evictForRatingChange(Book book) {
        Cache bookById = cacheManager.getCache("bookById");
        if (bookById != null) {
            bookById.evict(book.getId());
        }
        evictBookLists();
        evictAuthorBooks(book.getAuthors());
    }

    private void evictBookLists() {
        Cache bookLists = cacheManager.getCache("bookLists");
        if (bookLists != null) {
            bookLists.clear();
        }
    }

    private void evictAuthorBooks(Set<Author> authors) {
        Cache authorBooks = cacheManager.getCache("authorBooks");
        if (authorBooks == null) {
            return;
        }
        for (Author author : authors) {
            authorBooks.evict(author.getId());
        }
    }

    private Set<Author> resolveOrCreateAuthors(List<String> rawNames) {
        Set<Author> authors = new HashSet<>();
        for (String rawName : rawNames) {
            if (!StringUtils.hasText(rawName)) {
                continue;
            }
            String name = TextNormalizer.normalize(rawName.trim());
            // Only reuses an existing PUBLISHED author by name - never a stranger's not-yet-
            // approved draft, which would silently attach it to this book before it's been reviewed.
            Author author = authorRepository.findByNameIgnoreCaseAndStatus(name, com.bookdb.model.AuthorStatus.PUBLISHED)
                    .orElseGet(() -> {
                Author a = new Author();
                a.setName(name);
                a.setSlug(name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-"));
                return authorRepository.save(a);
            });
            authors.add(author);
        }
        return authors;
    }

    private boolean isOwner(Book book, User user) {
        return book.getSubmittedBy() != null && book.getSubmittedBy().getId().equals(user.getId());
    }

    private boolean isAdmin(User user) {
        return user.getRole() == com.bookdb.model.Role.ADMIN;
    }

    private void requireOwner(Book book, User user) {
        if (!isOwner(book, user)) {
            throw new ForbiddenException("You do not own this book");
        }
    }
}
