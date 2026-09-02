package com.bookdb.bootstrap;

import com.bookdb.model.Role;
import com.bookdb.model.User;
import com.bookdb.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Creates the first admin account on startup if no admin exists yet, from
 * {@code app.admin.email}/{@code app.admin.password}. Those are dev-only defaults in
 * application.properties - change them before running this anywhere but a local machine.
 */
@Component
@RequiredArgsConstructor
@Order(1)
public class AdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByRole(Role.ADMIN)) {
            return;
        }
        User admin = new User();
        admin.setEmail(adminEmail.trim().toLowerCase());
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setDisplayName("Admin");
        admin.setRole(Role.ADMIN);
        admin.setEnabled(true);
        admin.setCreatedAt(Instant.now());
        userRepository.save(admin);
        log.info("Seeded initial admin account: {}", admin.getEmail());
    }
}
