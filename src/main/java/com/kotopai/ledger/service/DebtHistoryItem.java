package com.kotopai.ledger.service;

import java.math.BigDecimal;
import java.time.Instant;

import com.kotopai.ledger.domain.DebtOrigin;
import com.kotopai.ledger.domain.DebtStatus;

public record DebtHistoryItem(
		long debtId,
		Long outingId,
		long debtorId,
		long creditorId,
		BigDecimal amount,
		BigDecimal remaining,
		DebtStatus status,
		DebtOrigin origin,
		Instant createdAt) {
}
