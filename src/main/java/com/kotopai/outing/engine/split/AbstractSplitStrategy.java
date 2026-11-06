package com.kotopai.outing.engine.split;

import com.kotopai.common.money.Money;
import com.kotopai.outing.engine.model.ItemLine;
import com.kotopai.outing.engine.model.ParticipantRef;
import com.kotopai.outing.engine.model.ShareLine;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Template Method: {@link #split} runs the checks every strategy needs (right
 * type, at least one consumer, no duplicates), then hands over to
 * {@link #doSplit} for the strategy-specific part.
 */
public abstract class AbstractSplitStrategy implements SplitStrategy {

    @Override
    public final Map<ParticipantRef, Money> split(ItemLine item) {
        Objects.requireNonNull(item, "item must not be null");
        if (item.splitType() != type()) {
            throw new IllegalArgumentException(
                "%s strategy cannot split %s item '%s'".formatted(type(), item.splitType(), item.name()));
        }
        if (item.shares().isEmpty()) {
            throw new IllegalArgumentException("item '%s' has no consumers".formatted(item.name()));
        }
        Set<ParticipantRef> seen = new HashSet<>();
        for (ShareLine share : item.shares()) {
            if (!seen.add(share.participant())) {
                throw new IllegalArgumentException(
                    "item '%s' lists %s more than once".formatted(item.name(), share.participant()));
            }
        }
        return Collections.unmodifiableMap(doSplit(item));
    }

    /** Strategy-specific split; the item is already checked by {@link #split}. */
    protected abstract Map<ParticipantRef, Money> doSplit(ItemLine item);

    /** Pairs each consumer with the share at the same position, keeping order. */
    protected static Map<ParticipantRef, Money> zip(List<ShareLine> shares, List<Money> amounts) {
        Map<ParticipantRef, Money> result = new LinkedHashMap<>();
        for (int i = 0; i < shares.size(); i++) {
            result.put(shares.get(i).participant(), amounts.get(i));
        }
        return result;
    }
}
