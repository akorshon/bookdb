package com.bookdb.web;

import com.bookdb.dto.AuthDtos.AuthResponse;
import com.bookdb.dto.AuthDtos.LoginRequest;
import com.bookdb.dto.AuthDtos.RegisterRequest;
import com.bookdb.dto.UserSummary;
import com.bookdb.model.User;
import com.bookdb.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public UserSummary me(@AuthenticationPrincipal User user) {
        return UserSummary.from(user);
    }
}
