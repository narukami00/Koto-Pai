package com.kotopai.ledger.event;

import java.math.BigDecimal;

public record DelegationExecutedEvent(
		long delegationId,
		long incomingDebtId,
		long outgoingDebtId,
		long delegatedDebtId,
		long debtorId,
		long middlemanId,
		long creditorId,
		BigDecimal amount) {
}
