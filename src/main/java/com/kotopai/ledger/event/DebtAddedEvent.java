package com.kotopai.ledger.event;

import java.math.BigDecimal;

public record DebtAddedEvent(
		long debtId,
		long outingId,
		long debtorId,
		long creditorId,
		BigDecimal amount) {
}
