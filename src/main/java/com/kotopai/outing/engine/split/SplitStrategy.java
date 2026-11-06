package com.kotopai.outing.engine.split;

import com.kotopai.common.money.Money;
import com.kotopai.outing.engine.model.ItemLine;
import com.kotopai.outing.engine.model.ParticipantRef;
import java.util.Map;

/**
 * Strategy pattern: one implementation per way of dividing an item's cost.
 * The rest of the engine only knows this interface, so a new split method is
 * a new class rather than another branch inside the calculator.
 */
public interface SplitStrategy {

    /** The split type this strategy handles. */
    SplitType type();

    /**
     * @return each consumer's share, in the item's consumer order; shares add up exactly to the item amount
     * @throws IllegalArgumentException if the item cannot be split this way
     */
    Map<ParticipantRef, Money> split(ItemLine item);
}
