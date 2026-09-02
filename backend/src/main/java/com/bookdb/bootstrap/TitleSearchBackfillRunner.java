package com.bookdb.bootstrap;

import com.bookdb.model.Book;
import com.bookdb.repository.BookRepository;
import com.bookdb.util.Transliteration;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * One-time maintenance pass: computes {@code Book.titleSearch} (the Latin phonetic search key,
 * see {@link Transliteration}) for existing rows that don't have one yet - pure local
 * computation from the already-stored title, no scraping needed. Run with
 * {@code --backfill-title-search}.
 */
@Component
@RequiredArgsConstructor
public class TitleSearchBackfillRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TitleSearchBackfillRunner.class);

    private final BookRepository bookRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!args.containsOption("backfill-title-search")) {
            return;
        }
        int updated = 0;
        for (Book book : bookRepository.findAll()) {
            if (book.getTitleSearch() != null) {
                continue;
            }
            book.setTitleSearch(Transliteration.toLatinSearchKey(book.getTitle()));
            updated++;
        }
        log.info("Backfilled titleSearch on {} book(s)", updated);
    }
}
