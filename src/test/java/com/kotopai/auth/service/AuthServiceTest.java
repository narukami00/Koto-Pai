package com.kotopai.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.kotopai.auth.dto.RegisterRequest;
import com.kotopai.common.exception.DuplicateResourceException;
import com.kotopai.user.domain.Role;
import com.kotopai.user.domain.User;
import com.kotopai.user.dto.UserSummary;
import com.kotopai.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private DefaultAuthService authService;

    @Test
    @DisplayName("register encodes password and saves user")
    void register_success() {
        RegisterRequest request = new RegisterRequest("rafsan10", "Rafsan Riasat", "rafsan@example.com", "mypassword123");

        when(userRepository.existsByUserCode("rafsan10")).thenReturn(false);
        when(userRepository.existsByEmail("rafsan@example.com")).thenReturn(false);
        when(passwordEncoder.encode("mypassword123")).thenReturn("encoded_bcrypt_hash");

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "id", 1L);
            return user;
        });

        UserSummary summary = authService.register(request);

        assertThat(summary.id()).isEqualTo(1L);
        assertThat(summary.userCode()).isEqualTo("rafsan10");
        assertThat(summary.displayName()).isEqualTo("Rafsan Riasat");
        verify(passwordEncoder).encode("mypassword123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("register throws DuplicateResourceException when userCode is taken")
    void register_duplicateUserCode() {
        RegisterRequest request = new RegisterRequest("rafsan10", "Rafsan Riasat", "rafsan@example.com", "pass123");
        when(userRepository.existsByUserCode("rafsan10")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Username 'rafsan10' is already taken");
    }

    @Test
    @DisplayName("register throws DuplicateResourceException when email is taken")
    void register_duplicateEmail() {
        RegisterRequest request = new RegisterRequest("rafsan10", "Rafsan Riasat", "rafsan@example.com", "pass123");
        when(userRepository.existsByUserCode("rafsan10")).thenReturn(false);
        when(userRepository.existsByEmail("rafsan@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Email 'rafsan@example.com' is already registered");
    }
}

