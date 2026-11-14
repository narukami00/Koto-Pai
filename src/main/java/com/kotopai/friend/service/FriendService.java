package com.kotopai.friend.service;

import java.util.List;

import com.kotopai.user.dto.UserSummary;

/**
 * Service for friendship management and relationship queries.
 *
 * <p>Shared cross-team contract required by Member 2 (outing participant validation)
 * and Member 3 (debt delegation validation).
 */
public interface FriendService {

    /**
     * Checks if two users have an accepted friendship.
     *
     * @param userA first user's ID
     * @param userB second user's ID
     * @return true if an accepted friendship exists symmetrically, false otherwise
     */
    boolean areFriends(Long userA, Long userB);

    /**
     * Returns the list of all accepted friends for a user.
     *
     * @param userId user's ID
     * @return list of UserSummary projections for accepted friends
     */
    List<UserSummary> friendsOf(Long userId);

    /**
     * Sends a friend request to a target user identified by public userCode.
     *
     * @param requesterId the initiating user's ID
     * @param targetUserCode public userCode of the recipient
     * @return the ID of the created pending friendship
     */
    Long sendFriendRequest(Long requesterId, String targetUserCode);

    /**
     * Accepts an incoming pending friend request.
     *
     * @param addresseeId the recipient's user ID
     * @param friendshipId the ID of the friendship record to accept
     */
    void acceptFriendRequest(Long addresseeId, Long friendshipId);

    /**
     * Rejects an incoming pending friend request.
     *
     * @param addresseeId the recipient's user ID
     * @param friendshipId the ID of the friendship record to reject
     */
    void rejectFriendRequest(Long addresseeId, Long friendshipId);

    /**
     * Removes an existing accepted friendship between two users.
     *
     * @param userId the initiating user's ID
     * @param friendId the friend's user ID
     */
    void removeFriend(Long userId, Long friendId);
}

