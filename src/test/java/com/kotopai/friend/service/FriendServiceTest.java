package com.kotopai.friend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.kotopai.common.exception.DuplicateResourceException;
import com.kotopai.common.exception.ForbiddenException;
import com.kotopai.common.exception.ResourceNotFoundException;
import com.kotopai.friend.domain.Friendship;
import com.kotopai.friend.domain.FriendshipStatus;
import com.kotopai.friend.dto.FriendRequestSummary;
import com.kotopai.friend.repository.FriendshipRepository;
import com.kotopai.user.domain.Role;
import com.kotopai.user.domain.User;
import com.kotopai.user.dto.UserSummary;
import com.kotopai.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class FriendServiceTest {

    @Mock
    private FriendshipRepository friendshipRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DefaultFriendService friendService;

    private User createUser(Long id, String userCode, String displayName) {
        User user = new User(userCode, displayName, userCode + "@example.com", "hash", Role.USER);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @Test
    @DisplayName("areFriends returns true when accepted friendship exists in either direction")
    void areFriends_true() {
        when(friendshipRepository.existsBetweenWithStatus(1L, 2L, FriendshipStatus.ACCEPTED)).thenReturn(true);

        assertThat(friendService.areFriends(1L, 2L)).isTrue();
    }

    @Test
    @DisplayName("areFriends returns false for null IDs, same ID, or missing friendship")
    void areFriends_false() {
        assertThat(friendService.areFriends(null, 2L)).isFalse();
        assertThat(friendService.areFriends(1L, null)).isFalse();
        assertThat(friendService.areFriends(1L, 1L)).isFalse();

        when(friendshipRepository.existsBetweenWithStatus(1L, 2L, FriendshipStatus.ACCEPTED)).thenReturn(false);
        assertThat(friendService.areFriends(1L, 2L)).isFalse();
    }

    @Test
    @DisplayName("friendsOf extracts counterparty summaries correctly")
    void friendsOf_success() {
        User me = createUser(1L, "rafsan10", "Rafsan");
        User friend1 = createUser(2L, "tanvir01", "Tanvir");
        User friend2 = createUser(3L, "nabila02", "Nabila");

        Friendship f1 = new Friendship(me, friend1); // me is requester
        f1.accept();
        Friendship f2 = new Friendship(friend2, me); // me is addressee
        f2.accept();

        when(friendshipRepository.findAcceptedForUser(1L)).thenReturn(List.of(f1, f2));

        List<UserSummary> friends = friendService.friendsOf(1L);

        assertThat(friends).hasSize(2);
        assertThat(friends).extracting(UserSummary::userCode).containsExactly("tanvir01", "nabila02");
    }

    @Test
    @DisplayName("pendingRequestsFor returns incoming pending requests for addressee")
    void pendingRequestsFor_success() {
        User requester = createUser(2L, "tanvir01", "Tanvir");
        User addressee = createUser(1L, "rafsan10", "Rafsan");

        Friendship pending = new Friendship(requester, addressee);
        ReflectionTestUtils.setField(pending, "id", 55L);

        when(friendshipRepository.findPendingRequestsForAddressee(1L)).thenReturn(List.of(pending));

        List<FriendRequestSummary> requests = friendService.pendingRequestsFor(1L);

        assertThat(requests).hasSize(1);
        assertThat(requests.get(0).friendshipId()).isEqualTo(55L);
        assertThat(requests.get(0).requesterId()).isEqualTo(2L);
        assertThat(requests.get(0).userCode()).isEqualTo("tanvir01");
        assertThat(requests.get(0).displayName()).isEqualTo("Tanvir");
        assertThat(requests.get(0).getInitial()).isEqualTo("T");
    }

    @Test
    @DisplayName("sendFriendRequest successfully strips leading @ symbol and normalizes code")
    void sendFriendRequest_withAtPrefix() {
        User requester = createUser(1L, "rafsan10", "Rafsan");
        User addressee = createUser(2L, "tanvir01", "Tanvir");

        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
        when(userRepository.findByUserCode("tanvir01")).thenReturn(Optional.of(addressee));
        when(friendshipRepository.findBetween(1L, 2L)).thenReturn(Optional.empty());

        Friendship saved = new Friendship(requester, addressee);
        ReflectionTestUtils.setField(saved, "id", 101L);
        when(friendshipRepository.save(any(Friendship.class))).thenReturn(saved);

        Long friendshipId = friendService.sendFriendRequest(1L, "  @Tanvir01  ");

        assertThat(friendshipId).isEqualTo(101L);
        verify(userRepository).findByUserCode("tanvir01");
    }

    @Test
    @DisplayName("sendFriendRequest successfully creates pending friendship")
    void sendFriendRequest_success() {
        User requester = createUser(1L, "rafsan10", "Rafsan");
        User addressee = createUser(2L, "tanvir01", "Tanvir");

        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
        when(userRepository.findByUserCode("tanvir01")).thenReturn(Optional.of(addressee));
        when(friendshipRepository.findBetween(1L, 2L)).thenReturn(Optional.empty());

        Friendship saved = new Friendship(requester, addressee);
        ReflectionTestUtils.setField(saved, "id", 100L);
        when(friendshipRepository.save(any(Friendship.class))).thenReturn(saved);

        Long friendshipId = friendService.sendFriendRequest(1L, "tanvir01");

        assertThat(friendshipId).isEqualTo(100L);
        verify(friendshipRepository).save(any(Friendship.class));
    }

    @Test
    @DisplayName("sendFriendRequest throws when sending request to oneself")
    void sendFriendRequest_self() {
        User requester = createUser(1L, "rafsan10", "Rafsan");
        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
        when(userRepository.findByUserCode("rafsan10")).thenReturn(Optional.of(requester));

        assertThatThrownBy(() -> friendService.sendFriendRequest(1L, "rafsan10"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot send a friend request to yourself");
    }

    @Test
    @DisplayName("sendFriendRequest throws DuplicateResourceException when relationship already exists")
    void sendFriendRequest_duplicate() {
        User requester = createUser(1L, "rafsan10", "Rafsan");
        User addressee = createUser(2L, "tanvir01", "Tanvir");

        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
        when(userRepository.findByUserCode("tanvir01")).thenReturn(Optional.of(addressee));
        when(friendshipRepository.findBetween(1L, 2L)).thenReturn(Optional.of(new Friendship(requester, addressee)));

        assertThatThrownBy(() -> friendService.sendFriendRequest(1L, "tanvir01"))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("acceptFriendRequest sets status to ACCEPTED")
    void acceptFriendRequest_success() {
        User requester = createUser(1L, "rafsan10", "Rafsan");
        User addressee = createUser(2L, "tanvir01", "Tanvir");
        Friendship friendship = new Friendship(requester, addressee);

        when(friendshipRepository.findById(10L)).thenReturn(Optional.of(friendship));

        friendService.acceptFriendRequest(2L, 10L);

        assertThat(friendship.getStatus()).isEqualTo(FriendshipStatus.ACCEPTED);
    }

    @Test
    @DisplayName("acceptFriendRequest rejects when caller is not the addressee")
    void acceptFriendRequest_forbidden() {
        User requester = createUser(1L, "rafsan10", "Rafsan");
        User addressee = createUser(2L, "tanvir01", "Tanvir");
        Friendship friendship = new Friendship(requester, addressee);

        when(friendshipRepository.findById(10L)).thenReturn(Optional.of(friendship));

        assertThatThrownBy(() -> friendService.acceptFriendRequest(1L, 10L)) // 1L is requester, not addressee!
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Only the addressee can accept");
    }

    @Test
    @DisplayName("rejectFriendRequest deletes pending friendship")
    void rejectFriendRequest_success() {
        User requester = createUser(1L, "rafsan10", "Rafsan");
        User addressee = createUser(2L, "tanvir01", "Tanvir");
        Friendship friendship = new Friendship(requester, addressee);

        when(friendshipRepository.findById(10L)).thenReturn(Optional.of(friendship));

        friendService.rejectFriendRequest(2L, 10L);

        verify(friendshipRepository).delete(friendship);
    }

    @Test
    @DisplayName("removeFriend deletes accepted friendship")
    void removeFriend_success() {
        User u1 = createUser(1L, "rafsan10", "Rafsan");
        User u2 = createUser(2L, "tanvir01", "Tanvir");
        Friendship friendship = new Friendship(u1, u2);
        friendship.accept();

        when(friendshipRepository.findBetween(1L, 2L)).thenReturn(Optional.of(friendship));

        friendService.removeFriend(1L, 2L);

        verify(friendshipRepository).delete(friendship);
    }
}

