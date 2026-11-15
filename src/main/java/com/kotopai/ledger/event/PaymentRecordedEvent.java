package com.kotopai.ledger.event;

import java.math.BigDecimal;

import com.kotopai.ledger.domain.PaymentStatus;

public record PaymentRecordedEvent(
		long paymentId,
		long debtId,
		Long outingId,
		long recordedById,
		long debtorId,
		long creditorId,
		BigDecimal amount,
		PaymentStatus status) {
}
