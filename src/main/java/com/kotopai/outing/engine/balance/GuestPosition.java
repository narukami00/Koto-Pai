package com.kotopai.outing.engine.balance;

import com.kotopai.common.money.Money;
import java.util.Objects;

/** A guest net kept separately, so the owner-guest record (what the guest owes the owner) can be created. */
public record GuestPosition(long guestId, long ownerUserId, Money net) {

    public GuestPosition {
        Objects.requireNonNull(net, "net must not be null");
    }
}
