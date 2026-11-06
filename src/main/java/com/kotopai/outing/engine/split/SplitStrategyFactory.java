package com.kotopai.outing.engine.split;

import com.kotopai.outing.engine.allocation.LargestRemainderAllocator;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Factory (registry): the one place that maps a {@link SplitType} to its
 * {@link SplitStrategy}. Callers depend only on the interface and never
 * instantiate concrete strategies. Construction fails fast if any split type
 * has no strategy or has two, so a missing mapping is found at startup rather
 * than when a user submits a bill.
 */
public final class SplitStrategyFactory {

    private final Map<SplitType, SplitStrategy> strategies = new EnumMap<>(SplitType.class);

    public SplitStrategyFactory(Collection<? extends SplitStrategy> strategies) {
        Objects.requireNonNull(strategies, "strategies must not be null");
        for (SplitStrategy strategy : strategies) {
            Objects.requireNonNull(strategy, "strategies must not contain null");
            SplitStrategy previous = this.strategies.put(strategy.type(), strategy);
            if (previous != null) {
                throw new IllegalStateException("two strategies registered for " + strategy.type()
                    + ": " + previous.getClass().getSimpleName() + " and " + strategy.getClass().getSimpleName());
            }
        }
        Set<SplitType> missing = EnumSet.allOf(SplitType.class);
        missing.removeAll(this.strategies.keySet());
        if (!missing.isEmpty()) {
            throw new IllegalStateException("no strategy registered for " + missing);
        }
    }

    /** Factory with the standard strategies, sharing one allocator. */
    public static SplitStrategyFactory withDefaultStrategies() {
        LargestRemainderAllocator allocator = new LargestRemainderAllocator();
        return new SplitStrategyFactory(List.of(
            new SoleConsumerSplitStrategy(),
            new EqualSplitStrategy(allocator),
            new PercentageSplitStrategy(allocator)));
    }

    public SplitStrategy forType(SplitType type) {
        return strategies.get(Objects.requireNonNull(type, "type must not be null"));
    }
}
