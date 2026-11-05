package com.kotopai.ledger.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

final class LedgerAmounts {

	static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

	private LedgerAmounts() {
	}

	static BigDecimal exact(BigDecimal amount, String field) {
		Objects.requireNonNull(amount, field + " must not be null");
		try {
			return amount.setScale(2, RoundingMode.UNNECESSARY);
		} catch (ArithmeticException exception) {
			throw new IllegalArgumentException(field + " must have at most 2 decimal places", exception);
		}
	}

	static BigDecimal positive(BigDecimal amount, String field) {
		BigDecimal normalized = exact(amount, field);
		if (normalized.signum() <= 0) {
			throw new IllegalArgumentException(field + " must be positive");
		}
		return normalized;
	}

	static long positiveId(Long id, String field) {
		if (id == null || id <= 0) {
			throw new IllegalArgumentException(field + " must be positive");
		}
		return id;
	}
}
