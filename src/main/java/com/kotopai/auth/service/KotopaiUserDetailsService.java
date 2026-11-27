package com.kotopai.auth.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kotopai.user.domain.User;
import com.kotopai.user.repository.UserRepository;

/**
 * User lookup service for Spring Security authentication.
 *
 * <p>Supports authenticating with either public {@code userCode} or registered {@code email}.
 */
@Service
public class KotopaiUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public KotopaiUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username == null || username.isBlank()) {
            throw new UsernameNotFoundException("Username or email must not be blank");
        }

        String normalized = username.trim().toLowerCase();
        User user = userRepository.findByUserCode(normalized)
                .or(() -> userRepository.findByEmail(normalized))
                .orElseThrow(() -> new UsernameNotFoundException("User not found with identifier: " + username));

        return new KotopaiUserDetails(user);
    }
}

