package com.kotopai.ledger.spi;

import java.util.List;

/** All calculated registered-user and guest nets for one active outing. */
public record OutingSettlementSnapshot(
		long outingId,
		List<UserNetPosition> userPositions,
		List<GuestNetPosition> guestPositions) {

	public OutingSettlementSnapshot {
		if (outingId <= 0) {
			throw new IllegalArgumentException("outingId must be positive");
		}
		userPositions = List.copyOf(userPositions);
		guestPositions = List.copyOf(guestPositions);
	}
}
