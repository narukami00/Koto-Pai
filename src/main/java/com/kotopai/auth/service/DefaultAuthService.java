package com.kotopai.auth.service;

import java.util.Objects;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kotopai.auth.dto.RegisterRequest;
import com.kotopai.common.exception.DuplicateResourceException;
import com.kotopai.user.domain.Role;
import com.kotopai.user.domain.User;
import com.kotopai.user.dto.UserSummary;
import com.kotopai.user.repository.UserRepository;

@Service
@Transactional
public class DefaultAuthService implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DefaultAuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserSummary register(RegisterRequest request) {
        Objects.requireNonNull(request, "request must not be null");

        String normalizedCode = request.userCode().trim().toLowerCase();
        if (userRepository.existsByUserCode(normalizedCode)) {
            throw new DuplicateResourceException("Username '" + request.userCode() + "' is already taken");
        }

        String normalizedEmail = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateResourceException("Email '" + request.email() + "' is already registered");
        }

        String passwordHash = passwordEncoder.encode(request.password());

        User user = new User(
                normalizedCode,
                request.displayName().trim(),
                normalizedEmail,
                passwordHash,
                Role.USER
        );

        User saved = userRepository.save(user);
        return UserSummary.from(saved);
    }
}

