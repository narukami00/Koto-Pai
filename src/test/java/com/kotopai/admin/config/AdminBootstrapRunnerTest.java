package com.kotopai.admin.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.kotopai.user.domain.Role;
import com.kotopai.user.domain.User;
import com.kotopai.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapRunnerTest {

    private static final String GOOD_PASSWORD = "Str0ng-passphrase";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AdminBootstrapRunner runner(String userCode, String email, String password, String displayName) {
        return new AdminBootstrapRunner(userRepository, passwordEncoder, userCode, email, password, displayName);
    }

    @Test
    @DisplayName("Does nothing when no admin user code is configured")
    void noConfiguration_doesNothing() {
        runner("", "", "", "Administrator").run();
        runner(null, null, null, null).run();
        runner("   ", "a@b.co", GOOD_PASSWORD, "X").run();

        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test
    @DisplayName("Promotes an existing regular user to ADMIN without touching the password")
    void existingUser_isPromoted() {
        User existing = new User("boot_admin", "Boot Admin", "boot@example.com", "existing-hash", Role.USER);
        when(userRepository.findByUserCode("boot_admin")).thenReturn(Optional.of(existing));

        runner("boot_admin", "", "", "").run();

        assertThat(existing.getRole()).isEqualTo(Role.ADMIN);
        assertThat(existing.getPasswordHash()).isEqualTo("existing-hash");
        verify(userRepository).save(existing);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("Leaves a user who is already an ADMIN alone")
    void existingAdmin_isLeftAlone() {
        User existing = new User("boot_admin", "Boot Admin", "boot@example.com", "existing-hash", Role.ADMIN);
        when(userRepository.findByUserCode("boot_admin")).thenReturn(Optional.of(existing));

        runner("boot_admin", "", "", "").run();

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Creates an ADMIN with a hashed password when the user does not exist")
    void missingUser_isCreated() {
        when(userRepository.findByUserCode("boot_admin")).thenReturn(Optional.empty());
        when(userRepository.existsByEmail("boss@example.com")).thenReturn(false);
        when(passwordEncoder.encode(GOOD_PASSWORD)).thenReturn("hashed-secret");

        runner("boot_admin", "boss@example.com", GOOD_PASSWORD, "The Boss").run();

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        User admin = saved.getValue();
        assertThat(admin.getUserCode()).isEqualTo("boot_admin");
        assertThat(admin.getEmail()).isEqualTo("boss@example.com");
        assertThat(admin.getDisplayName()).isEqualTo("The Boss");
        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
        assertThat(admin.getPasswordHash()).isEqualTo("hashed-secret").isNotEqualTo(GOOD_PASSWORD);
    }

    @Test
    @DisplayName("Trims and lower-cases the configured user code, and defaults the display name")
    void userCodeIsNormalised_displayNameDefaults() {
        when(userRepository.findByUserCode("boot_admin")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(GOOD_PASSWORD)).thenReturn("hashed-secret");

        runner("  Boot_Admin ", "boss@example.com", GOOD_PASSWORD, "  ").run();

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getUserCode()).isEqualTo("boot_admin");
        assertThat(saved.getValue().getDisplayName()).isEqualTo("Administrator");
    }

    @Test
    @DisplayName("Does not create the admin when the password is shorter than the minimum")
    void shortPassword_isRejected() {
        when(userRepository.findByUserCode("boot_admin")).thenReturn(Optional.empty());

        runner("boot_admin", "boss@example.com", "1234567", "Boss").run();

        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("Does not create the admin when no e-mail is configured")
    void missingEmail_isRejected() {
        when(userRepository.findByUserCode("boot_admin")).thenReturn(Optional.empty());

        runner("boot_admin", "", GOOD_PASSWORD, "Boss").run();

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Does not create the admin when the e-mail already belongs to someone else")
    void emailAlreadyTaken_isRejected() {
        when(userRepository.findByUserCode("boot_admin")).thenReturn(Optional.empty());
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        runner("boot_admin", "taken@example.com", GOOD_PASSWORD, "Boss").run();

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("A configuration the User rules reject is logged, never thrown, so the app still starts")
    void invalidUserCode_doesNotCrashStartup() {
        when(userRepository.findByUserCode("a!")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(GOOD_PASSWORD)).thenReturn("hashed-secret");

        assertThatCode(() -> runner("a!", "boss@example.com", GOOD_PASSWORD, "Boss").run()).doesNotThrowAnyException();

        verify(userRepository, never()).save(any());
    }
}
