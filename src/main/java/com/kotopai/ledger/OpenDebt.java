package com.kotopai.ledger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/** Immutable view of an open debt used by the pure delegation algorithms. */
public record OpenDebt(long id, long debtorId, long creditorId, BigDecimal remaining) {

	public OpenDebt {
		if (id <= 0 || debtorId <= 0 || creditorId <= 0) {
			throw new IllegalArgumentException("debt and user IDs must be positive");
		}
		if (debtorId == creditorId) {
			throw new IllegalArgumentException("debtor and creditor must be different users");
		}
		remaining = exactMoney(remaining, "remaining");
		if (remaining.signum() <= 0) {
			throw new IllegalArgumentException("remaining must be positive");
		}
	}

	static BigDecimal exactMoney(BigDecimal amount, String field) {
		Objects.requireNonNull(amount, field + " must not be null");
		try {
			return amount.setScale(2, RoundingMode.UNNECESSARY);
		} catch (ArithmeticException exception) {
			throw new IllegalArgumentException(field + " must have at most 2 decimal places", exception);
		}
	}
}
