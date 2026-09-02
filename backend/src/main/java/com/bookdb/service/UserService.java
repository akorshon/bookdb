package com.bookdb.service;

import com.bookdb.model.Role;
import com.bookdb.model.User;
import com.bookdb.repository.UserRepository;
import com.bookdb.web.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public Page<User> findAll(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Transactional
    public User setEnabled(Long id, boolean enabled) {
        User user = get(id);
        user.setEnabled(enabled);
        return user;
    }

    @Transactional
    public User promote(Long id) {
        User user = get(id);
        user.setRole(Role.ADMIN);
        return user;
    }

    private User get(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
    }
}
