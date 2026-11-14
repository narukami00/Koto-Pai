package com.kotopai.outing.port;

/**
 * Port (Dependency Inversion): what the outing feature needs to know about friendships, defined by the outing
 * feature and implemented by another team's code. The outing service depends on this interface only, so it can be
 * built and tested before Member 1's friend service exists.
 */
public interface FriendshipChecker {

    /** True if the two users are accepted friends. A user is never a friend of themselves. */
    boolean areFriends(Long userA, Long userB);
}
