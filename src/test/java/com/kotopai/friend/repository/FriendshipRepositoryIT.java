package com.kotopai.friend.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import com.kotopai.friend.domain.Friendship;
import com.kotopai.friend.domain.FriendshipStatus;
import com.kotopai.user.domain.Role;
import com.kotopai.user.domain.User;
import com.kotopai.user.repository.UserRepository;

@SpringBootTest
@Transactional
class FriendshipRepositoryIT {

    @Autowired
    private FriendshipRepository friendshipRepository;

    @Autowired
    private UserRepository userRepository;

    private User user1;
    private User user2;
    private User user3;

    @BeforeEach
    void setUp() {
        user1 = userRepository.saveAndFlush(new User("user_one", "User One", "u1@test.com", "hash", Role.USER));
        user2 = userRepository.saveAndFlush(new User("user_two", "User Two", "u2@test.com", "hash", Role.USER));
        user3 = userRepository.saveAndFlush(new User("user_three", "User Three", "u3@test.com", "hash", Role.USER));
    }

    @Test
    @DisplayName("findBetween finds friendship regardless of order")
    void findBetween_symmetric() {
        Friendship f = friendshipRepository.saveAndFlush(new Friendship(user1, user2));

        Optional<Friendship> found1 = friendshipRepository.findBetween(user1.getId(), user2.getId());
        Optional<Friendship> found2 = friendshipRepository.findBetween(user2.getId(), user1.getId());

        assertThat(found1).isPresent().contains(f);
        assertThat(found2).isPresent().contains(f);
    }

    @Test
    @DisplayName("existsBetweenWithStatus checks status in both directions")
    void existsBetweenWithStatus_symmetric() {
        Friendship f = new Friendship(user1, user2);
        f.accept();
        friendshipRepository.saveAndFlush(f);

        assertThat(friendshipRepository.existsBetweenWithStatus(user1.getId(), user2.getId(), FriendshipStatus.ACCEPTED))
                .isTrue();
        assertThat(friendshipRepository.existsBetweenWithStatus(user2.getId(), user1.getId(), FriendshipStatus.ACCEPTED))
                .isTrue();

        assertThat(friendshipRepository.existsBetweenWithStatus(user1.getId(), user2.getId(), FriendshipStatus.PENDING))
                .isFalse();
        assertThat(friendshipRepository.existsBetweenWithStatus(user1.getId(), user3.getId(), FriendshipStatus.ACCEPTED))
                .isFalse();
    }

    @Test
    @DisplayName("findAcceptedForUser returns bidirectional accepted friendships")
    void findAcceptedForUser() {
        Friendship f1 = new Friendship(user1, user2);
        f1.accept();
        friendshipRepository.saveAndFlush(f1);

        Friendship f2 = new Friendship(user3, user1);
        f2.accept();
        friendshipRepository.saveAndFlush(f2);

        List<Friendship> accepted = friendshipRepository.findAcceptedForUser(user1.getId());

        assertThat(accepted).hasSize(2);
    }

    @Test
    @DisplayName("findPendingRequestsForAddressee returns incoming pending requests")
    void findPendingRequestsForAddressee() {
        friendshipRepository.saveAndFlush(new Friendship(user1, user2)); // user1 requested user2
        friendshipRepository.saveAndFlush(new Friendship(user3, user2)); // user3 requested user2

        List<Friendship> pending = friendshipRepository.findPendingRequestsForAddressee(user2.getId());

        assertThat(pending).hasSize(2);
    }

    @Test
    @DisplayName("Unique constraint rejects duplicate friendship pair")
    void rejectsDuplicateFriendshipPair() {
        friendshipRepository.saveAndFlush(new Friendship(user1, user2));

        assertThatThrownBy(() -> friendshipRepository.saveAndFlush(new Friendship(user1, user2)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}

