package com.kotopai.ledger.service;

import java.math.BigDecimal;
import java.time.Instant;

import com.kotopai.ledger.domain.GuestDebt;
import com.kotopai.ledger.domain.GuestDebtStatus;

public record GuestDebtResult(
		long id,
		long outingId,
		long guestId,
		long ownerId,
		BigDecimal amount,
		GuestDebtStatus status,
		Instant settledAt,
		Instant createdAt) {

	public static GuestDebtResult from(GuestDebt debt) {
		return new GuestDebtResult(
				debt.getId(), debt.getOutingId(), debt.getGuestId(), debt.getOwnerId(),
				debt.getAmount(), debt.getStatus(), debt.getSettledAt(), debt.getCreatedAt());
	}
}
