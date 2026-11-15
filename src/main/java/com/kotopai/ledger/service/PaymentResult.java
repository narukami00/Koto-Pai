package com.kotopai.ledger.service;

import java.math.BigDecimal;
import java.time.Instant;

import com.kotopai.ledger.domain.Payment;
import com.kotopai.ledger.domain.PaymentStatus;

public record PaymentResult(
		long id,
		long debtId,
		BigDecimal amount,
		long recordedById,
		PaymentStatus status,
		Long confirmedById,
		Instant createdAt,
		Instant confirmedAt) {

	public static PaymentResult from(Payment payment) {
		return new PaymentResult(
				payment.getId(),
				payment.getDebt().getId(),
				payment.getAmount(),
				payment.getRecordedById(),
				payment.getStatus(),
				payment.getConfirmedById(),
				payment.getCreatedAt(),
				payment.getConfirmedAt());
	}
}
