package com.kotopai.common.config;

import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.kotopai.friend.domain.Friendship;
import com.kotopai.friend.repository.FriendshipRepository;
import com.kotopai.user.domain.Role;
import com.kotopai.user.domain.User;
import com.kotopai.user.repository.UserRepository;

/**
 * Idempotent development seed runner that populates canonical demo users and accepted
 * mutual friendships for local offline development (H2) and demo environments.
 *
 * <p>Active only under profiles {@code dev}, {@code h2}, or {@code demo}.
 */
@Component
@Profile({"dev", "h2", "demo"})
public class DevDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataInitializer.class);

    public static final String DEFAULT_PASSWORD = "password123";

    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;
    private final PasswordEncoder passwordEncoder;

    public DevDataInitializer(UserRepository userRepository,
                              FriendshipRepository friendshipRepository,
                              PasswordEncoder passwordEncoder) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.friendshipRepository = Objects.requireNonNull(friendshipRepository, "friendshipRepository must not be null");
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder, "passwordEncoder must not be null");
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.existsByUserCode("rafsan10")) {
            log.info("DevDataInitializer: Demo user 'rafsan10' already exists; skipping seed data initialization.");
            return;
        }

        String passwordHash = passwordEncoder.encode(DEFAULT_PASSWORD);

        User rafsan = userRepository.save(new User("rafsan10", "Rafsan", "rafsan@example.com", passwordHash, Role.USER));
        User tanvir = userRepository.save(new User("tanvir_h", "Tanvir", "tanvir@example.com", passwordHash, Role.USER));
        User nabila = userRepository.save(new User("nabila_r", "Nabila", "nabila@example.com", passwordHash, Role.USER));
        User samiha = userRepository.save(new User("samiha", "Samiha", "samiha@example.com", passwordHash, Role.USER));

        List<Friendship> mutualFriendships = List.of(
                acceptedFriendship(rafsan, tanvir),
                acceptedFriendship(rafsan, nabila),
                acceptedFriendship(rafsan, samiha),
                acceptedFriendship(tanvir, nabila),
                acceptedFriendship(tanvir, samiha),
                acceptedFriendship(nabila, samiha)
        );

        friendshipRepository.saveAll(mutualFriendships);

        log.info("DevDataInitializer: Successfully seeded 4 demo users ({}) and 6 accepted mutual friendships (password: '{}').",
                List.of(rafsan.getUserCode(), tanvir.getUserCode(), nabila.getUserCode(), samiha.getUserCode()),
                DEFAULT_PASSWORD);
    }

    private Friendship acceptedFriendship(User u1, User u2) {
        Friendship friendship = new Friendship(u1, u2);
        friendship.accept();
        return friendship;
    }
}

