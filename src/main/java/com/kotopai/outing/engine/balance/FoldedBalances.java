package com.kotopai.outing.engine.balance;

import com.kotopai.common.money.Money;
import java.util.List;

/**
 * Result of guest folding. {@code users} carry nets that already include their guests (these drive debts
 * between real users); {@code guests} keep each guest's own net (these drive owner-guest records).
 * The users' nets add up to the same total as before folding.
 */
public record FoldedBalances(List<UserPosition> users, List<GuestPosition> guests) {

    public FoldedBalances {
        users = List.copyOf(users);
        guests = List.copyOf(guests);
    }

    public Money usersTotal() {
        return Money.sum(users.stream().map(UserPosition::net).toList());
    }
}
