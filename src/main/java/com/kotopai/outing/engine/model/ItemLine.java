package com.kotopai.outing.engine.model;

import com.kotopai.common.money.Money;
import com.kotopai.outing.engine.split.SplitType;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** One item on the bill and who consumed it. Immutable. */
public record ItemLine(String name, Money amount, SplitType splitType, List<ShareLine> shares) {

    public ItemLine {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(splitType, "splitType must not be null");
        Objects.requireNonNull(shares, "shares must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("item name must not be blank");
        }
        if (amount.isNegative()) {
            throw new IllegalArgumentException("item amount must not be negative: " + amount);
        }
        shares = List.copyOf(shares);
    }

    /** Eaten by one person, e.g. a burger. */
    public static ItemLine personal(String name, Money amount, ParticipantRef consumer) {
        return new ItemLine(name, amount, SplitType.PERSONAL, List.of(ShareLine.of(consumer)));
    }

    /** Shared equally between the given people. */
    public static ItemLine equal(String name, Money amount, ParticipantRef... consumers) {
        return new ItemLine(name, amount, SplitType.EQUAL,
            Arrays.stream(consumers).map(ShareLine::of).toList());
    }

    /** Shared by estimated percentages, e.g. 50 / 30 / 20. */
    public static ItemLine percentage(String name, Money amount, ShareLine... shares) {
        return new ItemLine(name, amount, SplitType.PERCENTAGE, List.of(shares));
    }
}
