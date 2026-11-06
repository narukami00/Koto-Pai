package com.kotopai.outing.engine.split;

import com.kotopai.common.money.Money;
import com.kotopai.outing.engine.allocation.LargestRemainderAllocator;
import com.kotopai.outing.engine.model.ItemLine;
import com.kotopai.outing.engine.model.ParticipantRef;
import com.kotopai.outing.engine.model.ShareLine;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Shared item split by estimated percentages, which must add up to 100. */
public final class PercentageSplitStrategy extends AbstractSplitStrategy {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final LargestRemainderAllocator allocator;

    public PercentageSplitStrategy(LargestRemainderAllocator allocator) {
        this.allocator = Objects.requireNonNull(allocator, "allocator must not be null");
    }

    @Override
    public SplitType type() {
        return SplitType.PERCENTAGE;
    }

    @Override
    protected Map<ParticipantRef, Money> doSplit(ItemLine item) {
        List<BigDecimal> percentages = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (ShareLine share : item.shares()) {
            BigDecimal pct = share.percentage();
            if (pct == null) {
                throw new IllegalArgumentException("item '%s': no percentage for %s"
                    .formatted(item.name(), share.participant()));
            }
            if (pct.signum() < 0) {
                throw new IllegalArgumentException("item '%s': percentage must not be negative: %s"
                    .formatted(item.name(), pct));
            }
            percentages.add(pct);
            total = total.add(pct);
        }
        if (total.compareTo(HUNDRED) != 0) {
            throw new IllegalArgumentException("item '%s': percentages must add up to 100, got %s"
                .formatted(item.name(), total));
        }
        return zip(item.shares(), allocator.allocate(item.amount(), percentages));
    }
}
