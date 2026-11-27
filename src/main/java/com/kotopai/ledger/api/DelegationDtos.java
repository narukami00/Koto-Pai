package com.kotopai.ledger.api;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

final class DelegationDtos {

	private DelegationDtos() {
	}

	record ExecuteDelegationRequest(
			@Positive long incomingDebtId,
			@Positive long outgoingDebtId,
			@NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount) {
	}
}
