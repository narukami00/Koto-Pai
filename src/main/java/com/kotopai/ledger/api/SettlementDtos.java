package com.kotopai.ledger.api;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

final class SettlementDtos {

	private SettlementDtos() {
	}

	record RecordPaymentRequest(
			@NotNull
			@DecimalMin(value = "0.01")
			@Digits(integer = 17, fraction = 2)
			BigDecimal amount) {
	}

	record DisputeRequest(
			@NotBlank
			@Size(max = 500)
			String reason) {
	}
}
