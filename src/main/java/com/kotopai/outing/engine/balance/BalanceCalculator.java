package com.kotopai.outing.engine.balance;

import com.kotopai.common.money.Money;
import com.kotopai.outing.engine.model.ContributionLine;
import com.kotopai.outing.engine.model.ItemLine;
import com.kotopai.outing.engine.model.OutingSnapshot;
import com.kotopai.outing.engine.model.ParticipantRef;
import com.kotopai.outing.engine.split.SplitStrategyFactory;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Turns a snapshot into one {@link ParticipantBalance} per participant:
 * {@code consumed} = sum of that person's share of every item (split by the item's strategy),
 * {@code paid} = sum of their contributions, {@code net = paid - consumed}.
 *
 * <p>The calculator never branches on the split type; it asks the {@link SplitStrategyFactory}.
 * It assumes a snapshot that passed validation, but still refuses people who are not on the
 * outing, because silently inventing a participant would corrupt the books.
 */
public final class BalanceCalculator {

    private final SplitStrategyFactory strategies;

    public BalanceCalculator(SplitStrategyFactory strategies) {
        this.strategies = Objects.requireNonNull(strategies, "strategies must not be null");
    }

    public static BalanceCalculator withDefaultStrategies() {
        return new BalanceCalculator(SplitStrategyFactory.withDefaultStrategies());
    }

    /** @return balances in the snapshot's participant order; the nets always add up to zero when paid equals the bill */
    public List<ParticipantBalance> calculate(OutingSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Map<ParticipantRef, Money> consumed = zeroes(snapshot.participants());
        Map<ParticipantRef, Money> paid = zeroes(snapshot.participants());

        for (ItemLine item : snapshot.items()) {
            Map<ParticipantRef, Money> shares = strategies.forType(item.splitType()).split(item);
            shares.forEach((who, share) -> add(consumed, who, share, "consumes '" + item.name() + "'"));
        }
        for (ContributionLine contribution : snapshot.contributions()) {
            add(paid, contribution.participant(), contribution.amount(), "paid");
        }

        return snapshot.participants().stream()
            .map(p -> ParticipantBalance.of(p, consumed.get(p), paid.get(p)))
            .toList();
    }

    private static Map<ParticipantRef, Money> zeroes(List<ParticipantRef> participants) {
        Map<ParticipantRef, Money> map = new LinkedHashMap<>();
        participants.forEach(p -> map.put(p, Money.ZERO));
        return map;
    }

    private static void add(Map<ParticipantRef, Money> totals, ParticipantRef who, Money amount, String what) {
        Money current = totals.get(who);
        if (current == null) {
            throw new IllegalArgumentException(who.label() + " " + what + " but is not on the outing");
        }
        totals.put(who, current.plus(amount));
    }
}
