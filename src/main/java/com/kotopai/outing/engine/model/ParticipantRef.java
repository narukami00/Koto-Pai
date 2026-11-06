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

    /** Short human-readable name for error messages, e.g. "user #1" or "guest #10". */
    String label();

    record User(long userId) implements ParticipantRef {
        @Override
        public String label() {
            return "user #" + userId;
        }
    }

    record Guest(long guestId, long ownerUserId) implements ParticipantRef {
        @Override
        public String label() {
            return "guest #" + guestId;
        }
    }
}
