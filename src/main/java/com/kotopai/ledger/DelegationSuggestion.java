package com.kotopai.ledger;

import java.math.BigDecimal;

/** A maximum safe delegation through one middleman. */
public record DelegationSuggestion(
		long incomingDebtId,
		long outgoingDebtId,
		long debtorId,
		long middlemanId,
		long creditorId,
		BigDecimal amount) {
}
