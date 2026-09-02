package com.bookdb.dto;

import com.bookdb.model.Category;

public record CategorySummary(Long id, String name) {

    public static CategorySummary from(Category c) {
        return new CategorySummary(c.getId(), c.getName());
    }
}
