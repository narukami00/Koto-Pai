package com.kotopai.outing.engine.split;

import com.kotopai.common.money.Money;
import com.kotopai.outing.engine.allocation.LargestRemainderAllocator;
import com.kotopai.outing.engine.model.ItemLine;
import com.kotopai.outing.engine.model.ParticipantRef;
import java.util.Map;
import java.util.Objects;

/** Shared item split equally; leftover poisha go to the earliest consumers. */
public final class EqualSplitStrategy extends AbstractSplitStrategy {

    private final LargestRemainderAllocator allocator;

    public EqualSplitStrategy(LargestRemainderAllocator allocator) {
        this.allocator = Objects.requireNonNull(allocator, "allocator must not be null");
    }

    @Override
    public SplitType type() {
        return SplitType.EQUAL;
    }

    @Override
    protected Map<ParticipantRef, Money> doSplit(ItemLine item) {
        return zip(item.shares(), allocator.allocateEvenly(item.amount(), item.shares().size()));
    }
}
