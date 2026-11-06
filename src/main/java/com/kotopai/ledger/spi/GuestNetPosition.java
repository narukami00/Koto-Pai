package com.kotopai.ledger.spi;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/** Guest net from the split engine; it is folded into the owning user's net. */
public record GuestNetPosition(long guestId, long ownerId, BigDecimal net) {

	public GuestNetPosition {
		if (guestId <= 0 || ownerId <= 0) {
			throw new IllegalArgumentException("guestId and ownerId must be positive");
		}
		net = Objects.requireNonNull(net, "net must not be null").setScale(2, RoundingMode.HALF_EVEN);
	}
}
