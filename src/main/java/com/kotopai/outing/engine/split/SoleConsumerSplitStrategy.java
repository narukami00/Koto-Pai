package com.kotopai.outing.engine.split;

import com.kotopai.common.money.Money;
import com.kotopai.outing.engine.model.ItemLine;
import com.kotopai.outing.engine.model.ParticipantRef;
import java.util.LinkedHashMap;
import java.util.Map;

/** Personal item: the single consumer gets the whole amount. */
public final class SoleConsumerSplitStrategy extends AbstractSplitStrategy {

    @Override
    public SplitType type() {
        return SplitType.PERSONAL;
    }

    @Override
    protected Map<ParticipantRef, Money> doSplit(ItemLine item) {
        if (item.shares().size() != 1) {
            throw new IllegalArgumentException("personal item '%s' must have exactly one consumer, has %d"
                .formatted(item.name(), item.shares().size()));
        }
        Map<ParticipantRef, Money> result = new LinkedHashMap<>();
        result.put(item.shares().get(0).participant(), item.amount());
        return result;
    }
}
