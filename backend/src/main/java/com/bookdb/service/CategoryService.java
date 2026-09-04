package com.bookdb.service;

import com.bookdb.dto.CategoryDtos.CategoryCreateRequest;
import com.bookdb.dto.CategoryDtos.CategoryUpdateRequest;
import com.bookdb.dto.CategorySummary;
import com.bookdb.model.Book;
import com.bookdb.model.Category;
import com.bookdb.repository.BookRepository;
import com.bookdb.repository.CategoryRepository;
import com.bookdb.util.TextNormalizer;
import com.bookdb.web.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final BookRepository bookRepository;
    private final CacheManager cacheManager;

    @Cacheable("categories")
    public List<CategorySummary> listAll() {
        return categoryRepository.findAll().stream()
                .map(CategorySummary::from)
                .sorted(Comparator.comparing(CategorySummary::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /** Admin-only (see {@code AdminCategoryController}, gated by {@code /api/admin/**}). */
    @CacheEvict(value = "categories", allEntries = true)
    @Transactional
    public Category create(CategoryCreateRequest request) {
        String name = TextNormalizer.normalize(request.name().trim());
        categoryRepository.findByNameIgnoreCase(name).ifPresent(existing -> {
            throw new IllegalStateException("A category named '" + name + "' already exists");
        });
        Category category = new Category();
        category.setName(name);
        return categoryRepository.save(category);
    }

    /**
     * Admin-only rename. {@code Category} has no draft/moderation status - a category is always
     * "live" - so unlike {@code AuthorService.update}, there's no ownership/editability gate to
     * bypass; the whole endpoint is admin-only via the {@code /api/admin/**} security gate.
     * Since {@code Book.categories} is a many-to-many FK (not a name copy), renaming this one
     * row automatically reflects on every book referencing it - but that name is also embedded
     * (via {@code CategorySummary}) in cached {@code bookLists}/{@code bookById}/{@code authorBooks}
     * entries for those books, which is why this also clears those caches.
     */
    @CacheEvict(value = "categories", allEntries = true)
    @Transactional
    public Category rename(Long id, CategoryUpdateRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category not found"));
        String name = TextNormalizer.normalize(request.name().trim());
        categoryRepository.findByNameIgnoreCase(name).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new IllegalStateException("A category named '" + name + "' already exists");
            }
        });
        category.setName(name);
        evictBookCaches();
        return category;
    }

    /**
     * Admin-only. Unlike Author (whose delete refuses while still attached to books), a category
     * is just a tag - deleting it removes the tag from any books that had it rather than blocking,
     * so this also evicts the book caches that embed category names (see {@code evictBookCaches}).
     */
    @CacheEvict(value = "categories", allEntries = true)
    @Transactional
    public void delete(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category not found"));
        for (Book book : bookRepository.findDistinctByCategories_Id(id)) {
            book.getCategories().remove(category);
        }
        categoryRepository.delete(category);
        evictBookCaches();
    }

    private void evictBookCaches() {
        for (String cacheName : List.of("bookLists", "bookById", "authorBooks")) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        }
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
