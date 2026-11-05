package com.kotopai.ledger;

import java.math.BigDecimal;
import java.util.Objects;

/** Validates and calculates a debt delegation without mutating persistence state. */
public final class DelegationRules {

	private final FriendshipLookup friendships;

	public DelegationRules(FriendshipLookup friendships) {
		this.friendships = Objects.requireNonNull(friendships, "friendships must not be null");
	}

	public DelegationPlan plan(
			OpenDebt incomingDebt,
			OpenDebt outgoingDebt,
			long middlemanId,
			BigDecimal requestedAmount) {
		Objects.requireNonNull(incomingDebt, "incomingDebt must not be null");
		Objects.requireNonNull(outgoingDebt, "outgoingDebt must not be null");
		if (middlemanId <= 0) {
			throw new IllegalArgumentException("middlemanId must be positive");
		}
		if (incomingDebt.creditorId() != middlemanId) {
			throw new IllegalArgumentException("incoming debt must be owed to the middleman");
		}
		if (outgoingDebt.debtorId() != middlemanId) {
			throw new IllegalArgumentException("outgoing debt must be owed by the middleman");
		}
		if (incomingDebt.debtorId() == outgoingDebt.creditorId()) {
			throw new IllegalArgumentException("delegation cannot create a debt to the same user");
		}
		if (!friendships.areFriends(incomingDebt.debtorId(), outgoingDebt.creditorId())) {
			throw new IllegalArgumentException("delegation requires the outer users to be friends");
		}

		BigDecimal amount = OpenDebt.exactMoney(requestedAmount, "requestedAmount");
		if (amount.signum() <= 0) {
			throw new IllegalArgumentException("requestedAmount must be positive");
		}
		BigDecimal maximum = incomingDebt.remaining().min(outgoingDebt.remaining());
		if (amount.compareTo(maximum) > 0) {
			throw new IllegalArgumentException("requestedAmount exceeds the delegatable balance");
		}

		return new DelegationPlan(
				middlemanId,
				new DebtAdjustment(
						incomingDebt.id(), incomingDebt.remaining(), incomingDebt.remaining().subtract(amount)),
				new DebtAdjustment(
						outgoingDebt.id(), outgoingDebt.remaining(), outgoingDebt.remaining().subtract(amount)),
				new ProposedDebt(incomingDebt.debtorId(), outgoingDebt.creditorId(), amount));
	}
}
