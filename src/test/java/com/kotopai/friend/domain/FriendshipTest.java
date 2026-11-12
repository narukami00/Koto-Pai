package com.kotopai.friend.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.kotopai.user.domain.Role;
import com.kotopai.user.domain.User;

class FriendshipTest {

    @Test
    @DisplayName("Creates friendship with PENDING status")
    void createFriendship_success() {
        User u1 = new User("user1", "User One", "u1@example.com", "hash", Role.USER);
        User u2 = new User("user2", "User Two", "u2@example.com", "hash", Role.USER);

        Friendship friendship = new Friendship(u1, u2);

        assertThat(friendship.getRequester()).isEqualTo(u1);
        assertThat(friendship.getAddressee()).isEqualTo(u2);
        assertThat(friendship.getStatus()).isEqualTo(FriendshipStatus.PENDING);
    }

    @Test
    @DisplayName("Rejects friendship when requester and addressee are identical")
    void rejectsSelfFriendship() {
        User u1 = new User("user1", "User One", "u1@example.com", "hash", Role.USER);

        assertThatThrownBy(() -> new Friendship(u1, u1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot create friendship with oneself");
    }

    @Test
    @DisplayName("accept transitions status from PENDING to ACCEPTED")
    void accept_success() {
        User u1 = new User("user1", "User One", "u1@example.com", "hash", Role.USER);
        User u2 = new User("user2", "User Two", "u2@example.com", "hash", Role.USER);

        Friendship friendship = new Friendship(u1, u2);
        friendship.accept();

        assertThat(friendship.getStatus()).isEqualTo(FriendshipStatus.ACCEPTED);
    }

    @Test
    @DisplayName("accept throws IllegalStateException if already ACCEPTED")
    void accept_alreadyAccepted() {
        User u1 = new User("user1", "User One", "u1@example.com", "hash", Role.USER);
        User u2 = new User("user2", "User Two", "u2@example.com", "hash", Role.USER);

        Friendship friendship = new Friendship(u1, u2);
        friendship.accept();

        assertThatThrownBy(friendship::accept)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already accepted");
    }
}

