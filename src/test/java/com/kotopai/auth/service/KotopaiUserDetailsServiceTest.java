package com.kotopai.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import com.kotopai.user.domain.Role;
import com.kotopai.user.domain.User;
import com.kotopai.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class KotopaiUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private KotopaiUserDetailsService userDetailsService;

    private User createUser(Long id, String userCode, String email, Role role) {
        User user = new User(userCode, "Test User", email, "secret_hash", role);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @Test
    @DisplayName("loadUserByUsername loads user by userCode")
    void loadByUserCode() {
        User user = createUser(1L, "rafsan10", "rafsan@example.com", Role.USER);
        when(userRepository.findByUserCode("rafsan10")).thenReturn(Optional.of(user));

        UserDetails details = userDetailsService.loadUserByUsername("rafsan10");

        assertThat(details.getUsername()).isEqualTo("rafsan10");
        assertThat(details.getPassword()).isEqualTo("secret_hash");
        assertThat(details.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("loadUserByUsername loads user by email fallback")
    void loadByEmail() {
        User user = createUser(2L, "admin_user", "admin@kotopai.com", Role.ADMIN);
        when(userRepository.findByUserCode("admin@kotopai.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("admin@kotopai.com")).thenReturn(Optional.of(user));

        UserDetails details = userDetailsService.loadUserByUsername("admin@kotopai.com");

        assertThat(details.getUsername()).isEqualTo("admin_user");
        assertThat(details.getAuthorities()).extracting("authority").containsExactly("ROLE_ADMIN");
    }

    @Test
    @DisplayName("loadUserByUsername throws UsernameNotFoundException when user is not found")
    void userNotFound() {
        when(userRepository.findByUserCode("unknown")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("unknown"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found with identifier: unknown");
    }

    @Test
    @DisplayName("loadUserByUsername throws UsernameNotFoundException for blank input")
    void blankUsername() {
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("   "))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}

