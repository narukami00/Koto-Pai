package com.kotopai.ledger.event;

import java.math.BigDecimal;

public record PaymentConfirmedEvent(
		long paymentId,
		long debtId,
		Long outingId,
		long confirmedById,
		long debtorId,
		long creditorId,
		BigDecimal amount) {
}
