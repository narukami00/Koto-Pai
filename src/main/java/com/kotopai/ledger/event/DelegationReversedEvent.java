package com.kotopai.ledger.event;

import java.math.BigDecimal;

public record DelegationReversedEvent(
		long delegationId,
		long delegatedDebtId,
		long disputedById,
		long debtorId,
		long middlemanId,
		long creditorId,
		BigDecimal amount,
		String reason) {
}
