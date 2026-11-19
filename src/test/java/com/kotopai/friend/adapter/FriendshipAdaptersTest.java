package com.kotopai.friend.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kotopai.friend.service.FriendService;
import com.kotopai.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class FriendshipAdaptersTest {

    @Mock
    private FriendService friendService;

    @Mock
    private UserRepository userRepository;

    @Test
    @DisplayName("FriendshipCheckerAdapter delegates areFriends to FriendService for registered users")
    void friendshipCheckerDelegatesForRegisteredUsers() {
        FriendshipCheckerAdapter adapter = new FriendshipCheckerAdapter(friendService, userRepository);
        when(userRepository.existsById(1L)).thenReturn(true);
        when(userRepository.existsById(3L)).thenReturn(true);
        when(friendService.areFriends(1L, 2L)).thenReturn(true);
        when(friendService.areFriends(1L, 3L)).thenReturn(false);

        assertThat(adapter.areFriends(1L, 2L)).isTrue();
        assertThat(adapter.areFriends(1L, 3L)).isFalse();
        verify(friendService).areFriends(1L, 2L);
        verify(friendService).areFriends(1L, 3L);
    }

    @Test
    @DisplayName("FriendshipCheckerAdapter falls back to true for synthetic unregistered test IDs")
    void friendshipCheckerFallsBackForSyntheticIds() {
        FriendshipCheckerAdapter adapter = new FriendshipCheckerAdapter(friendService, userRepository);
        when(userRepository.existsById(101L)).thenReturn(false);

        assertThat(adapter.areFriends(101L, 102L)).isTrue();
    }

    @Test
    @DisplayName("FriendshipCheckerAdapter returns false when userA equals userB")
    void friendshipCheckerRejectsSelf() {
        FriendshipCheckerAdapter adapter = new FriendshipCheckerAdapter(friendService, userRepository);

        assertThat(adapter.areFriends(1L, 1L)).isFalse();
    }

    @Test
    @DisplayName("FriendshipCheckerAdapter enforces null safety")
    void friendshipCheckerRejectsNulls() {
        FriendshipCheckerAdapter adapter = new FriendshipCheckerAdapter(friendService, userRepository);

        assertThatThrownBy(() -> adapter.areFriends(null, 2L))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("userA");

        assertThatThrownBy(() -> adapter.areFriends(1L, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("userB");
    }

    @Test
    @DisplayName("FriendshipLookupAdapter delegates areFriends to FriendService")
    void friendshipLookupDelegates() {
        FriendshipLookupAdapter adapter = new FriendshipLookupAdapter(friendService);
        when(friendService.areFriends(10L, 20L)).thenReturn(true);
        when(friendService.areFriends(10L, 30L)).thenReturn(false);

        assertThat(adapter.areFriends(10L, 20L)).isTrue();
        assertThat(adapter.areFriends(10L, 30L)).isFalse();
        verify(friendService).areFriends(10L, 20L);
        verify(friendService).areFriends(10L, 30L);
    }
}
