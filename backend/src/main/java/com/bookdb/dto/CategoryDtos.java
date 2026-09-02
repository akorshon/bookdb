package com.bookdb.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class CategoryDtos {

    private CategoryDtos() {
    }

    public record CategoryCreateRequest(@NotBlank @Size(max = 200) String name) {
    }

    public record CategoryUpdateRequest(@NotBlank @Size(max = 200) String name) {
    }
}
