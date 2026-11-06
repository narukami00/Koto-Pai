package com.kotopai.ledger;

/** Cross-team port implemented by Member 1's FriendService adapter. */
@FunctionalInterface
public interface FriendshipLookup {

	boolean areFriends(long userA, long userB);
}
