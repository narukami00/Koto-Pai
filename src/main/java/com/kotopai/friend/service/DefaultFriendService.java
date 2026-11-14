package com.kotopai.friend.service;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kotopai.common.exception.DuplicateResourceException;
import com.kotopai.common.exception.ForbiddenException;
import com.kotopai.common.exception.ResourceNotFoundException;
import com.kotopai.friend.domain.Friendship;
import com.kotopai.friend.domain.FriendshipStatus;
import com.kotopai.friend.repository.FriendshipRepository;
import com.kotopai.user.domain.User;
import com.kotopai.user.dto.UserSummary;
import com.kotopai.user.repository.UserRepository;

@Service
@Transactional
public class DefaultFriendService implements FriendService {

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;

    public DefaultFriendService(FriendshipRepository friendshipRepository, UserRepository userRepository) {
        this.friendshipRepository = friendshipRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean areFriends(Long userA, Long userB) {
        if (userA == null || userB == null || userA.equals(userB)) {
            return false;
        }
        return friendshipRepository.existsBetweenWithStatus(userA, userB, FriendshipStatus.ACCEPTED);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSummary> friendsOf(Long userId) {
        Objects.requireNonNull(userId, "userId must not be null");
        List<Friendship> friendships = friendshipRepository.findAcceptedForUser(userId);

        return friendships.stream()
                .map(f -> {
                    User friend = f.getRequester().getId().equals(userId)
                            ? f.getAddressee()
                            : f.getRequester();
                    return UserSummary.from(friend);
                })
                .toList();
    }

    @Override
    public Long sendFriendRequest(Long requesterId, String targetUserCode) {
        Objects.requireNonNull(requesterId, "requesterId must not be null");
        if (targetUserCode == null || targetUserCode.isBlank()) {
            throw new IllegalArgumentException("targetUserCode must not be blank");
        }

        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new ResourceNotFoundException("User", requesterId));

        String normalizedCode = targetUserCode.trim().toLowerCase();
        User addressee = userRepository.findByUserCode(normalizedCode)
                .orElseThrow(() -> new ResourceNotFoundException("User", "userCode", targetUserCode));

        if (requester.getId().equals(addressee.getId())) {
            throw new IllegalArgumentException("cannot send a friend request to yourself");
        }

        if (friendshipRepository.findBetween(requester.getId(), addressee.getId()).isPresent()) {
            throw new DuplicateResourceException("Friendship or friend request already exists between users");
        }

        Friendship friendship = new Friendship(requester, addressee);
        Friendship saved = friendshipRepository.save(friendship);
        return saved.getId();
    }

    @Override
    public void acceptFriendRequest(Long addresseeId, Long friendshipId) {
        Objects.requireNonNull(addresseeId, "addresseeId must not be null");
        Objects.requireNonNull(friendshipId, "friendshipId must not be null");

        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new ResourceNotFoundException("Friendship", friendshipId));

        if (!friendship.getAddressee().getId().equals(addresseeId)) {
            throw new ForbiddenException("Only the addressee can accept this friend request");
        }

        friendship.accept();
    }

    @Override
    public void rejectFriendRequest(Long addresseeId, Long friendshipId) {
        Objects.requireNonNull(addresseeId, "addresseeId must not be null");
        Objects.requireNonNull(friendshipId, "friendshipId must not be null");

        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new ResourceNotFoundException("Friendship", friendshipId));

        if (!friendship.getAddressee().getId().equals(addresseeId)) {
            throw new ForbiddenException("Only the addressee can reject this friend request");
        }

        friendshipRepository.delete(friendship);
    }

    @Override
    public void removeFriend(Long userId, Long friendId) {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(friendId, "friendId must not be null");

        Friendship friendship = friendshipRepository.findBetween(userId, friendId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No friendship found between user " + userId + " and " + friendId));

        if (friendship.getStatus() != FriendshipStatus.ACCEPTED) {
            throw new ResourceNotFoundException(
                    "No accepted friendship found between user " + userId + " and " + friendId);
        }

        friendshipRepository.delete(friendship);
    }
}

