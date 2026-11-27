package com.kotopai.outing.engine;

import com.kotopai.common.money.Money;
import com.kotopai.outing.engine.balance.FoldedBalances;
import com.kotopai.outing.engine.balance.ParticipantBalance;
import java.util.List;
import java.util.Objects;

/**
 * Everything the engine worked out for one outing.
 *
 * @param billTotal sum of all items
 * @param balances  one entry per participant (guests included), before folding
 * @param folded    nets per real user (guests folded into owners) plus each guest own net
 */
public record EngineResult(Money billTotal, List<ParticipantBalance> balances, FoldedBalances folded) {

    public EngineResult {
        Objects.requireNonNull(billTotal, "billTotal must not be null");
        Objects.requireNonNull(folded, "folded must not be null");
        balances = List.copyOf(balances);
    }
}
