package com.kotopai.outing.adapter;

import com.kotopai.outing.port.FriendshipChecker;
import java.util.Objects;

/**
 * Temporary stand-in until Member 1 friend service is on develop (M2-28): every pair of different users counts as
 * friends, so the outing feature can be developed and demonstrated. It is registered only when no real
 * {@link FriendshipChecker} exists, see {@code OutingPortsConfig}. Delete it when the real adapter lands.
 */
public class StubFriendshipChecker implements FriendshipChecker {

    @Override
    public boolean areFriends(Long userA, Long userB) {
        Objects.requireNonNull(userA, "userA must not be null");
        Objects.requireNonNull(userB, "userB must not be null");
        return !userA.equals(userB);
    }
}
