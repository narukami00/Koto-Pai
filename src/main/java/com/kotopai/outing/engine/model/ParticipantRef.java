package com.kotopai.outing.engine.model;

/**
 * Who took part in an outing, as the engine sees it: either a registered user
 * or a guest owned by a user (the guest's balance is later folded into the owner).
 */
public sealed interface ParticipantRef {

    static ParticipantRef user(long userId) {
        return new User(userId);
    }

    static ParticipantRef guest(long guestId, long ownerUserId) {
        return new Guest(guestId, ownerUserId);
    }

    record User(long userId) implements ParticipantRef {
    }

    record Guest(long guestId, long ownerUserId) implements ParticipantRef {
    }
}
