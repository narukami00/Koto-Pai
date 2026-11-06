package com.kotopai.ledger.domain;

/** State-pattern contract for legal debt lifecycle transitions. */
public interface DebtState {

	DebtStatus status();

	boolean canTransitionTo(DebtStatus target);

	default void requireTransitionTo(DebtStatus target) {
		if (!canTransitionTo(target)) {
			throw new IllegalStateException("cannot transition debt from " + status() + " to " + target);
		}
	}
}
