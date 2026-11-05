package com.kotopai.ledger.service;

import java.math.BigDecimal;
import java.util.List;

public record LedgerSummary(
		long userId,
		BigDecimal totalReceivable,
		BigDecimal totalPayable,
		BigDecimal net,
		List<LedgerBalance> balances) {

	public LedgerSummary {
		balances = List.copyOf(balances);
	}

	public static LedgerSummary empty(long userId) {
		BigDecimal zero = BigDecimal.ZERO.setScale(2);
		return new LedgerSummary(userId, zero, zero, zero, List.of());
	}
}
