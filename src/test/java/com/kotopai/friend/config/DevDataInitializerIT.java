package com.kotopai.friend.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import com.kotopai.common.config.DevDataInitializer;
import com.kotopai.friend.domain.Friendship;
import com.kotopai.friend.domain.FriendshipStatus;
import com.kotopai.friend.repository.FriendshipRepository;
import com.kotopai.ledger.FriendshipLookup;
import com.kotopai.outing.port.FriendshipChecker;
import com.kotopai.user.domain.Role;
import com.kotopai.user.domain.User;
import com.kotopai.user.repository.UserRepository;

@SpringBootTest
@ActiveProfiles("h2")
class DevDataInitializerIT {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FriendshipRepository friendshipRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private FriendshipChecker friendshipChecker;

    @Autowired
    private FriendshipLookup friendshipLookup;

    @Autowired
    private DevDataInitializer initializer;

    @Test
    @DisplayName("Seeds 4 canonical users with encrypted passwords and 6 mutual accepted friendships")
    void seedDataLoadsSuccessfully() {
        assertThat(userRepository.count()).isGreaterThanOrEqualTo(4);

        User rafsan = userRepository.findByUserCode("rafsan10").orElseThrow();
        User tanvir = userRepository.findByUserCode("tanvir_h").orElseThrow();
        User nabila = userRepository.findByUserCode("nabila_r").orElseThrow();
        User samiha = userRepository.findByUserCode("samiha").orElseThrow();

        assertThat(rafsan.getDisplayName()).isEqualTo("Rafsan");
        assertThat(rafsan.getEmail()).isEqualTo("rafsan@example.com");
        assertThat(rafsan.getRole()).isEqualTo(Role.USER);
        assertThat(passwordEncoder.matches("password123", rafsan.getPasswordHash())).isTrue();

        assertThat(tanvir.getDisplayName()).isEqualTo("Tanvir");
        assertThat(tanvir.getEmail()).isEqualTo("tanvir@example.com");

        assertThat(nabila.getDisplayName()).isEqualTo("Nabila");
        assertThat(nabila.getEmail()).isEqualTo("nabila@example.com");

        assertThat(samiha.getDisplayName()).isEqualTo("Samiha");
        assertThat(samiha.getEmail()).isEqualTo("samiha@example.com");

        // Verify mutual friendships via FriendshipChecker port
        assertThat(friendshipChecker.areFriends(rafsan.getId(), tanvir.getId())).isTrue();
        assertThat(friendshipChecker.areFriends(tanvir.getId(), rafsan.getId())).isTrue();

        assertThat(friendshipChecker.areFriends(rafsan.getId(), nabila.getId())).isTrue();
        assertThat(friendshipChecker.areFriends(nabila.getId(), rafsan.getId())).isTrue();

        assertThat(friendshipChecker.areFriends(rafsan.getId(), samiha.getId())).isTrue();
        assertThat(friendshipChecker.areFriends(samiha.getId(), rafsan.getId())).isTrue();

        assertThat(friendshipChecker.areFriends(tanvir.getId(), nabila.getId())).isTrue();
        assertThat(friendshipChecker.areFriends(tanvir.getId(), samiha.getId())).isTrue();
        assertThat(friendshipChecker.areFriends(nabila.getId(), samiha.getId())).isTrue();

        // Verify via FriendshipLookup port
        assertThat(friendshipLookup.areFriends(rafsan.getId(), tanvir.getId())).isTrue();
        assertThat(friendshipLookup.areFriends(nabila.getId(), samiha.getId())).isTrue();

        // Self is not friend
        assertThat(friendshipChecker.areFriends(rafsan.getId(), rafsan.getId())).isFalse();

        // Registered user who is not a friend returns false
        User stranger = userRepository.save(new User("stranger_test", "Stranger", "stranger@example.com", "hash", Role.USER));
        assertThat(friendshipChecker.areFriends(rafsan.getId(), stranger.getId())).isFalse();

        // All 6 seeded friendships in the DB have status ACCEPTED
        assertThat(friendshipRepository.findAll())
                .hasSize(6)
                .allMatch(f -> f.getStatus() == FriendshipStatus.ACCEPTED);
    }

    @Test
    @DisplayName("DevDataInitializer is idempotent: running it again does not duplicate or fail")
    void initializerIsIdempotent() {
        long userCountBefore = userRepository.count();
        long friendshipCountBefore = friendshipRepository.count();

        // Run again
        initializer.run();

        assertThat(userRepository.count()).isEqualTo(userCountBefore);
        assertThat(friendshipRepository.count()).isEqualTo(friendshipCountBefore);
    }
}
