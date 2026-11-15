package com.kotopai.ledger.spi;

/** Ledger-facing port for the two outing operations needed by settlement flows. */
public interface OutingLedgerGateway {

	long creatorId(long outingId);

	void markSettled(long outingId);
}
