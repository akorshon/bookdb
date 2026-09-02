package com.bookdb.service;

import com.bookdb.dto.AuthDtos.AuthResponse;
import com.bookdb.dto.AuthDtos.LoginRequest;
import com.bookdb.dto.AuthDtos.RegisterRequest;
import com.bookdb.dto.UserSummary;
import com.bookdb.model.Role;
import com.bookdb.model.User;
import com.bookdb.repository.UserRepository;
import com.bookdb.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new DataIntegrityViolationException("An account with this email already exists");
        }
        User user = new User();
        user.setEmail(request.email().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDisplayName(request.displayName().trim());
        user.setRole(Role.USER);
        user.setEnabled(true);
        user.setCreatedAt(Instant.now());
        user = userRepository.save(user);
        return new AuthResponse(jwtService.generateToken(user), UserSummary.from(user));
    }

    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        } catch (org.springframework.security.core.AuthenticationException e) {
            throw new BadCredentialsException("Invalid email or password");
        }
        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        return new AuthResponse(jwtService.generateToken(user), UserSummary.from(user));
    }
}
