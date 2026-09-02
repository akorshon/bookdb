package com.bookdb.bootstrap;

import com.bookdb.repository.AuthorRepository;
import com.bookdb.repository.BookRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import static com.bookdb.util.TextNormalizer.normalize;

/**
 * One-time maintenance pass: re-applies {@link com.bookdb.util.TextNormalizer} to rows already
 * in the database, for whenever the normalization form changes (see its Javadoc) after some data
 * has already been imported. Run with {@code --normalize-existing}.
 */
@Component
public class NormalizeExistingDataRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(NormalizeExistingDataRunner.class);

    private final AuthorRepository authorRepository;
    private final BookRepository bookRepository;

    public NormalizeExistingDataRunner(AuthorRepository authorRepository, BookRepository bookRepository) {
        this.authorRepository = authorRepository;
        this.bookRepository = bookRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!args.containsOption("normalize-existing")) {
            return;
        }

        var authors = authorRepository.findAll();
        authors.forEach(a -> {
            a.setName(normalize(a.getName()));
            a.setBio(normalize(a.getBio()));
        });
        authorRepository.saveAll(authors);

        var books = bookRepository.findAll();
        books.forEach(b -> {
            b.setTitle(normalize(b.getTitle()));
            b.setEdition(normalize(b.getEdition()));
            b.setSummary(normalize(b.getSummary()));
        });
        bookRepository.saveAll(books);

        log.info("Normalized {} authors and {} books", authors.size(), books.size());
    }
}
