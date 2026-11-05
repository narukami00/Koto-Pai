package com.kotopai.ledger;

/** The two reductions and one new debt produced by a valid delegation. */
public record DelegationPlan(
		long middlemanId,
		DebtAdjustment incomingDebt,
		DebtAdjustment outgoingDebt,
		ProposedDebt delegatedDebt) {
}
