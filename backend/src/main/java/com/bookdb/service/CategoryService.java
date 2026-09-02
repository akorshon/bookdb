package com.bookdb.service;

import com.bookdb.dto.CategorySummary;
import com.bookdb.model.Category;
import com.bookdb.repository.CategoryRepository;
import com.bookdb.util.TextNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Cacheable("categories")
    public List<CategorySummary> listAll() {
        return categoryRepository.findAll().stream()
                .map(CategorySummary::from)
                .sorted(Comparator.comparing(CategorySummary::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /**
     * Find-or-create by name, used by both {@code BookService} (user-submitted books) and
     * {@code ScrapeImportService} (scraped books) - callers are responsible for evicting the
     * {@code categories} cache when this creates new rows (both already do, via their own
     * enclosing {@code @CacheEvict}).
     */
    public Set<Category> findOrCreateByNames(List<String> rawNames) {
        if (rawNames == null) {
            return new HashSet<>();
        }
        Set<Category> categories = new HashSet<>();
        for (String rawName : rawNames) {
            if (!StringUtils.hasText(rawName)) {
                continue;
            }
            String name = TextNormalizer.normalize(rawName.trim());
            Category category = categoryRepository.findByNameIgnoreCase(name).orElseGet(() -> {
                Category c = new Category();
                c.setName(name);
                return categoryRepository.save(c);
            });
            categories.add(category);
        }
        return categories;
    }
}
