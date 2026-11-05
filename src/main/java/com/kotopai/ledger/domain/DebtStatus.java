package com.kotopai.ledger.domain;

/** Persisted debt state; each enum constant owns its legal transitions. */
public enum DebtStatus implements DebtState {
	OPEN {
		@Override
		public boolean canTransitionTo(DebtStatus target) {
			return target == PARTIALLY_PAID || target == PAID || target == DISPUTED || target == VOID;
		}
	},
	PARTIALLY_PAID {
		@Override
		public boolean canTransitionTo(DebtStatus target) {
			return target == PAID || target == DISPUTED || target == VOID;
		}
	},
	PAID {
		@Override
		public boolean canTransitionTo(DebtStatus target) {
			return target == VOID;
		}
	},
	DISPUTED {
		@Override
		public boolean canTransitionTo(DebtStatus target) {
			return target == OPEN || target == VOID;
		}
	},
	VOID {
		@Override
		public boolean canTransitionTo(DebtStatus target) {
			return false;
		}
	};

	@Override
	public DebtStatus status() {
		return this;
	}

	public boolean contributesToBalance() {
		return this == OPEN || this == PARTIALLY_PAID;
	}
}
