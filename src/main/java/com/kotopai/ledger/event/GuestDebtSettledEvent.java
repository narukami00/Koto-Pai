package com.kotopai.ledger.event;

import java.math.BigDecimal;

public record GuestDebtSettledEvent(
		long guestDebtId,
		long outingId,
		long guestId,
		long ownerId,
		BigDecimal amount) {
}
