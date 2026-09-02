package com.bookdb.web;

import com.bookdb.dto.CategoryDtos.CategoryCreateRequest;
import com.bookdb.dto.CategoryDtos.CategoryUpdateRequest;
import com.bookdb.dto.CategorySummary;
import com.bookdb.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategorySummary create(@Valid @RequestBody CategoryCreateRequest request) {
        return CategorySummary.from(categoryService.create(request));
    }

    @PutMapping("/{id}")
    public CategorySummary rename(@PathVariable Long id, @Valid @RequestBody CategoryUpdateRequest request) {
        return CategorySummary.from(categoryService.rename(id, request));
    }
}
