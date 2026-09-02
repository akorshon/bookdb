package com.bookdb.web;

import com.bookdb.dto.UserSummary;
import com.bookdb.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    public Page<UserSummary> list(Pageable pageable) {
        return userService.findAll(pageable).map(UserSummary::from);
    }

    @PostMapping("/{id}/disable")
    public UserSummary disable(@PathVariable Long id) {
        return UserSummary.from(userService.setEnabled(id, false));
    }

    @PostMapping("/{id}/enable")
    public UserSummary enable(@PathVariable Long id) {
        return UserSummary.from(userService.setEnabled(id, true));
    }

    @PostMapping("/{id}/promote")
    public UserSummary promote(@PathVariable Long id) {
        return UserSummary.from(userService.promote(id));
    }
}
