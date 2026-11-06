package com.kotopai.ledger.spi;

/**
 * Cross-team port. Member 2's outing service supplies its validated engine
 * snapshot through an adapter implementing this interface.
 */
@FunctionalInterface
public interface OutingSettlementProvider {

	OutingSettlementSnapshot settlementFor(long outingId);
}
