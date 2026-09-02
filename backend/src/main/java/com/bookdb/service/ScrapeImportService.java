package com.bookdb.service;

import com.bookdb.dto.ScrapeImportDtos.ScrapeExport;
import com.bookdb.dto.ScrapeImportDtos.ScrapeImportSummary;
import com.bookdb.dto.ScrapeImportDtos.ScrapedAuthor;
import com.bookdb.dto.ScrapeImportDtos.ScrapedBook;
import com.bookdb.model.Author;
import com.bookdb.model.AuthorStatus;
import com.bookdb.model.Book;
import com.bookdb.model.BookStatus;
import com.bookdb.repository.AuthorRepository;
import com.bookdb.repository.BookRepository;
import com.bookdb.util.TextNormalizer;
import com.bookdb.util.Transliteration;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Imports a JSON export produced by the standalone {@code bookdbscraper} project (see
 * {@code com.bookdb.dto.ScrapeImportDtos} for the shared shape) - the admin-panel replacement
 * for the old direct-to-DB scrape flow. Continues past a single bad row instead of aborting the
 * whole file, collecting failures into the returned summary.
 */
@Service
@RequiredArgsConstructor
public class ScrapeImportService {

    private final AuthorRepository authorRepository;
    private final BookRepository bookRepository;
    private final CategoryService categoryService;
    private final ObjectMapper objectMapper;

    @CacheEvict(value = {"authors", "categories", "bookById"}, allEntries = true)
    @Transactional
    public ScrapeImportSummary importJson(MultipartFile file) throws IOException {
        ScrapeExport export = objectMapper.readValue(file.getInputStream(), ScrapeExport.class);
        List<String> errors = new ArrayList<>();
        Instant now = Instant.now();

        Map<String, Author> authorsBySlug = new HashMap<>();
        int authorsCreated = 0;
        int authorsUpdated = 0;
        for (ScrapedAuthor scraped : export.authors()) {
            try {
                boolean isNew = authorRepository.findBySlug(scraped.slug()).isEmpty();
                Author author = upsertAuthor(scraped, now);
                authorsBySlug.put(scraped.slug(), author);
                if (isNew) {
                    authorsCreated++;
                } else {
                    authorsUpdated++;
                }
            } catch (Exception e) {
                errors.add("Author '" + scraped.slug() + "': " + e.getMessage());
            }
        }

        int booksCreated = 0;
        int booksUpdated = 0;
        for (ScrapedBook scraped : export.books()) {
            try {
                boolean isNew = bookRepository.findByDetailUrl(scraped.detailUrl()).isEmpty();
                upsertBook(scraped, authorsBySlug, now);
                if (isNew) {
                    booksCreated++;
                } else {
                    booksUpdated++;
                }
            } catch (Exception e) {
                errors.add("Book '" + scraped.detailUrl() + "': " + e.getMessage());
            }
        }

        return new ScrapeImportSummary(authorsCreated, authorsUpdated, booksCreated, booksUpdated, errors);
    }

    private Author upsertAuthor(ScrapedAuthor scraped, Instant now) {
        Author author = authorRepository.findBySlug(scraped.slug()).orElseGet(Author::new);
        author.setSlug(scraped.slug());
        author.setName(TextNormalizer.normalize(scraped.name()));
        author.setNameNative(TextNormalizer.normalize(scraped.nameNative()));
        author.setProfileUrl(scraped.profileUrl());
        author.setImageUrl(scraped.imageUrl());
        author.setBio(TextNormalizer.normalize(scraped.bio()));
        author.setSource(scraped.source());
        author.setStatus(AuthorStatus.PUBLISHED);
        author.setScrapedAt(now);
        return authorRepository.save(author);
    }

    private void upsertBook(ScrapedBook scraped, Map<String, Author> authorsBySlug, Instant now) {
        Book book = bookRepository.findByDetailUrl(scraped.detailUrl()).orElseGet(Book::new);
        String title = TextNormalizer.normalize(scraped.title());
        book.setTitle(title);
        book.setTitleSearch(Transliteration.toLatinSearchKey(title));
        book.setEdition(TextNormalizer.normalize(scraped.edition()));
        book.setDetailUrl(scraped.detailUrl());
        book.setThumbnailUrl(scraped.thumbnailUrl());
        book.setSummary(TextNormalizer.normalize(scraped.summary()));
        book.setCategories(categoryService.findOrCreateByNames(scraped.categories()));
        book.setAuthors(scraped.authorSlugs().stream()
                .map(slug -> authorsBySlug.computeIfAbsent(slug, s -> authorRepository.findBySlug(s).orElse(null)))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        book.setSource(scraped.source());
        book.setStatus(BookStatus.PUBLISHED);
        book.setScrapedAt(now);
        bookRepository.save(book);
    }
}
