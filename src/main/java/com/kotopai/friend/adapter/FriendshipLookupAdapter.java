package com.kotopai.friend.adapter;

import java.util.Objects;

import org.springframework.stereotype.Component;

import com.kotopai.friend.service.FriendService;
import com.kotopai.ledger.FriendshipLookup;

/**
 * Adapter implementing Member 3's {@link FriendshipLookup} port by delegating
 * to Member 1's {@link FriendService}.
 */
@Component
public class FriendshipLookupAdapter implements FriendshipLookup {

    private final FriendService friendService;

    public FriendshipLookupAdapter(FriendService friendService) {
        this.friendService = Objects.requireNonNull(friendService, "friendService must not be null");
    }

    @Override
    public boolean areFriends(long userA, long userB) {
        return friendService.areFriends(userA, userB);
    }
}

