package com.bookdb.bootstrap;

import com.bookdb.model.Author;
import com.bookdb.model.Book;
import com.bookdb.model.Category;
import com.bookdb.repository.BookRepository;
import com.bookdb.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * One-time maintenance pass: assigns genre categories to already-scraped books based on their
 * author, so the catalog has realistic category coverage to browse instead of only the handful
 * of categories picked on user-submitted test books. Run with {@code --seed-categories}. Only
 * fills in books that don't already have a category (won't touch anything a user picked).
 */
@Component
@RequiredArgsConstructor
public class CategorySeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CategorySeeder.class);

    private static final Map<String, List<String>> CATEGORIES_BY_AUTHOR = new HashMap<>();

    static {
        CATEGORIES_BY_AUTHOR.put("humayun ahmed", List.of("Fiction", "Science Fiction", "Thriller"));
        CATEGORIES_BY_AUTHOR.put("paulo coelho", List.of("Fiction", "Philosophy"));
        CATEGORIES_BY_AUTHOR.put("tamim shahriar subeen", List.of("Non-Fiction", "Technology"));
        CATEGORIES_BY_AUTHOR.put("syed shamsul haque", List.of("Poetry", "Classic Literature"));
        CATEGORIES_BY_AUTHOR.put("sunil gangapadhyay (nillohit)", List.of("Fiction", "Poetry"));
        CATEGORIES_BY_AUTHOR.put("stephen king", List.of("Horror", "Thriller"));
        CATEGORIES_BY_AUTHOR.put("sowkot osman", List.of("Classic Literature", "Fiction"));
        CATEGORIES_BY_AUTHOR.put("shirshendu mukhopadhyay", List.of("Fiction", "Children's Books"));
        CATEGORIES_BY_AUTHOR.put("selina hossain", List.of("Fiction", "Historical Fiction"));
        CATEGORIES_BY_AUTHOR.put("sayed mujtaba ali", List.of("Non-Fiction", "Humor"));
        CATEGORIES_BY_AUTHOR.put("satyajit ray", List.of("Mystery", "Children's Books"));
        CATEGORIES_BY_AUTHOR.put("samaresh majumder", List.of("Fiction", "Romance"));
        CATEGORIES_BY_AUTHOR.put("sadat hossain", List.of("Fiction", "Romance"));
        CATEGORIES_BY_AUTHOR.put("rokib hasan", List.of("Adventure", "Mystery"));
        CATEGORIES_BY_AUTHOR.put("rabindranath tagore", List.of("Poetry", "Classic Literature"));
        CATEGORIES_BY_AUTHOR.put("ahmed sofa", List.of("Classic Literature", "Non-Fiction"));
        CATEGORIES_BY_AUTHOR.put("manik bandyopadhyay", List.of("Classic Literature", "Fiction"));
        CATEGORIES_BY_AUTHOR.put("kazi nazrul islam", List.of("Poetry"));
        CATEGORIES_BY_AUTHOR.put("kazi anower hossain", List.of("Adventure", "Thriller"));
        CATEGORIES_BY_AUTHOR.put("jahir rayhan", List.of("Classic Literature", "Fiction"));
        CATEGORIES_BY_AUTHOR.put("j. k. rowling", List.of("Fantasy"));
        CATEGORIES_BY_AUTHOR.put("imdadul haq milan", List.of("Fiction", "Romance"));
        CATEGORIES_BY_AUTHOR.put("dan brown", List.of("Thriller", "Mystery"));
        CATEGORIES_BY_AUTHOR.put("chetan bhagat", List.of("Fiction", "Romance"));
        CATEGORIES_BY_AUTHOR.put("bibhutibhusan bandopadhyay", List.of("Classic Literature", "Fiction"));
        CATEGORIES_BY_AUTHOR.put("bankimacandro chattopadhay", List.of("Classic Literature"));
        CATEGORIES_BY_AUTHOR.put("anisul hoque", List.of("Fiction", "Non-Fiction"));
        CATEGORIES_BY_AUTHOR.put("akhtaruzzaman elias", List.of("Fiction", "Classic Literature"));
        CATEGORIES_BY_AUTHOR.put("sumanto aslam", List.of("Fiction", "Romance"));
    }

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    private final Map<String, Category> categoryCache = new HashMap<>();

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!args.containsOption("seed-categories")) {
            return;
        }

        int updated = 0;
        for (Book book : bookRepository.findAll()) {
            if (!book.getCategories().isEmpty()) {
                continue;
            }
            List<String> names = categoryNamesFor(book);
            if (names.isEmpty()) {
                continue;
            }
            for (String name : names) {
                book.getCategories().add(resolveOrCreateCategory(name));
            }
            updated++;
        }
        // @Transactional: no explicit save() needed, changes flush via dirty checking.
        log.info("Seeded categories on {} book(s)", updated);
    }

    private List<String> categoryNamesFor(Book book) {
        for (Author author : book.getAuthors()) {
            List<String> names = CATEGORIES_BY_AUTHOR.get(author.getName().toLowerCase(Locale.ROOT));
            if (names != null) {
                return names;
            }
        }
        return List.of();
    }

    private Category resolveOrCreateCategory(String name) {
        return categoryCache.computeIfAbsent(name.toLowerCase(Locale.ROOT), key ->
                categoryRepository.findByNameIgnoreCase(name).orElseGet(() -> {
                    Category c = new Category();
                    c.setName(name);
                    return categoryRepository.save(c);
                }));
    }
}
