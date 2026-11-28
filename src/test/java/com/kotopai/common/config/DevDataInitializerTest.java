package com.kotopai.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.kotopai.friend.repository.FriendshipRepository;
import com.kotopai.user.domain.Role;
import com.kotopai.user.domain.User;
import com.kotopai.user.repository.UserRepository;

/**
 * The demo seed must never put a well-known administrator on a hosted deployment: the {@code admin} account is
 * created for local runs ({@code dev}, {@code h2}) only.
 */
@ExtendWith(MockitoExtension.class)
class DevDataInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FriendshipRepository friendshipRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private List<User> seededWith(String... activeProfiles) {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(activeProfiles);
        when(passwordEncoder.encode(DevDataInitializer.DEFAULT_PASSWORD)).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));

        new DevDataInitializer(userRepository, friendshipRepository, passwordEncoder, environment).run();

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository, atLeastOnce()).save(saved.capture());
        return saved.getAllValues();
    }

    @Test
    @DisplayName("The h2 profile seeds the four demo users and the local demo admin")
    void h2_seedsAdmin() {
        List<User> users = seededWith("h2");

        assertThat(users).extracting(User::getUserCode)
                .containsExactlyInAnyOrder("rafsan10", "tanvir_h", "nabila_r", "samiha", "admin");
        assertThat(users).filteredOn(u -> u.getRole() == Role.ADMIN).extracting(User::getUserCode).containsExactly("admin");
    }

    @Test
    @DisplayName("The dev profile also seeds the local demo admin")
    void dev_seedsAdmin() {
        assertThat(seededWith("dev")).extracting(User::getUserCode).contains("admin");
    }

    @Test
    @DisplayName("A hosted prod+demo deployment gets the demo users but no published-password admin")
    void prodDemo_doesNotSeedAdmin() {
        List<User> users = seededWith("prod", "demo");

        assertThat(users).extracting(User::getUserCode)
                .containsExactlyInAnyOrder("rafsan10", "tanvir_h", "nabila_r", "samiha");
        assertThat(users).noneMatch(u -> u.getRole() == Role.ADMIN);
    }

    @Test
    @DisplayName("The demo profile on its own does not seed an admin either")
    void demoOnly_doesNotSeedAdmin() {
        assertThat(seededWith("demo")).noneMatch(u -> u.getRole() == Role.ADMIN);
    }

    @Test
    @DisplayName("Skips everything when the demo users already exist")
    void alreadySeeded_isSkipped() {
        when(userRepository.existsByUserCode("rafsan10")).thenReturn(true);

        new DevDataInitializer(userRepository, friendshipRepository, passwordEncoder, new MockEnvironment()).run();

        verify(userRepository, never()).save(any(User.class));
    }
}
