package com.kotopai.ledger.event;

public record DebtDisputedEvent(
		long debtId,
		long outingId,
		long disputedById,
		long outingCreatorId,
		long debtorId,
		long creditorId,
		String reason) {
}
