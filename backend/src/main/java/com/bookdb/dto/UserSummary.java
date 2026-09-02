package com.bookdb.dto;

import com.bookdb.model.User;

import java.time.Instant;

public record UserSummary(
        Long id,
        String email,
        String displayName,
        String role,
        boolean enabled,
        Instant createdAt) {

    public static UserSummary from(User u) {
        return new UserSummary(u.getId(), u.getEmail(), u.getDisplayName(), u.getRole().name(),
                u.isEnabled(), u.getCreatedAt());
    }
}
