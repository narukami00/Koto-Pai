package com.kotopai.ledger.spi;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/** Net for a registered outing participant; positive receives, negative owes. */
public record UserNetPosition(long userId, BigDecimal net) {

	public UserNetPosition {
		if (userId <= 0) {
			throw new IllegalArgumentException("userId must be positive");
		}
		net = Objects.requireNonNull(net, "net must not be null").setScale(2, RoundingMode.HALF_EVEN);
	}
}
