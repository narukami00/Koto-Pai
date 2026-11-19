package com.kotopai.ledger.service;

import java.math.BigDecimal;
import java.time.Instant;

import com.kotopai.ledger.domain.Delegation;
import com.kotopai.ledger.domain.DelegationStatus;

public record DelegationResult(
		long id,
		long incomingDebtId,
		long outgoingDebtId,
		long delegatedDebtId,
		long debtorId,
		long middlemanId,
		long creditorId,
		BigDecimal amount,
		DelegationStatus status,
		Instant createdAt,
		Instant reversedAt,
		Long reversedById,
		String reversalReason) {

	public static DelegationResult from(Delegation delegation) {
		return new DelegationResult(
				delegation.getId(), delegation.getIncomingDebtId(), delegation.getOutgoingDebtId(),
				delegation.getDelegatedDebtId(), delegation.getDebtorId(), delegation.getMiddlemanId(),
				delegation.getCreditorId(), delegation.getAmount(), delegation.getStatus(),
				delegation.getCreatedAt(), delegation.getReversedAt(), delegation.getReversedById(),
				delegation.getReversalReason());
	}
}
