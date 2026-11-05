package com.kotopai.ledger.domain;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "payments")
public class Payment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "debt_id", nullable = false)
	private Debt debt;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal amount;

	@Column(name = "recorded_by_id", nullable = false)
	private Long recordedById;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 24)
	private PaymentStatus status;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	protected Payment() {
	}

	private Payment(Debt debt, BigDecimal amount, Long recordedById, PaymentStatus status) {
		this.debt = java.util.Objects.requireNonNull(debt, "debt must not be null");
		this.amount = LedgerAmounts.positive(amount, "amount");
		this.recordedById = LedgerAmounts.positiveId(recordedById, "recordedById");
		this.status = java.util.Objects.requireNonNull(status, "status must not be null");
	}

	public static Payment pending(Debt debt, BigDecimal amount, Long recordedById) {
		return new Payment(debt, amount, recordedById, PaymentStatus.PENDING_CONFIRMATION);
	}

	public static Payment confirmed(Debt debt, BigDecimal amount, Long recordedById) {
		return new Payment(debt, amount, recordedById, PaymentStatus.CONFIRMED);
	}

	@PrePersist
	void initializeCreatedAt() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}

	public void confirm() {
		if (status != PaymentStatus.PENDING_CONFIRMATION) {
			throw new IllegalStateException("only pending payments can be confirmed");
		}
		status = PaymentStatus.CONFIRMED;
	}

	public Long getId() {
		return id;
	}

	public Debt getDebt() {
		return debt;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public Long getRecordedById() {
		return recordedById;
	}

	public PaymentStatus getStatus() {
		return status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
