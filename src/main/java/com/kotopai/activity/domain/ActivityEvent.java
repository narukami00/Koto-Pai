package com.kotopai.activity.domain;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "activity_events")
public class ActivityEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "actor_id", nullable = false)
	private Long actorId;

	@Column(name = "subject_user_id")
	private Long subjectUserId;

	@Column(name = "related_user_id")
	private Long relatedUserId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private ActivityType type;

	@Column(name = "outing_id")
	private Long outingId;

	@Column(name = "debt_id")
	private Long debtId;

	@Column(name = "delegation_id")
	private Long delegationId;

	@Column(precision = 19, scale = 2)
	private BigDecimal amount;

	@Column(nullable = false, length = 500)
	private String description;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	protected ActivityEvent() {
	}

	public ActivityEvent(
			long actorId,
			Long subjectUserId,
			Long relatedUserId,
			ActivityType type,
			Long outingId,
			Long debtId,
			Long delegationId,
			BigDecimal amount,
			String description) {
		if (actorId <= 0) {
			throw new IllegalArgumentException("actorId must be positive");
		}
		if (description == null || description.isBlank()) {
			throw new IllegalArgumentException("description must not be blank");
		}
		this.actorId = actorId;
		this.subjectUserId = subjectUserId;
		this.relatedUserId = relatedUserId;
		this.type = java.util.Objects.requireNonNull(type, "type must not be null");
		this.outingId = outingId;
		this.debtId = debtId;
		this.delegationId = delegationId;
		this.amount = amount == null ? null : amount.setScale(2, java.math.RoundingMode.UNNECESSARY);
		this.description = description.strip();
		if (this.description.length() > 500) {
			throw new IllegalArgumentException("description must not exceed 500 characters");
		}
	}

	@PrePersist
	void initializeCreatedAt() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}

	public Long getId() { return id; }
	public Long getActorId() { return actorId; }
	public Long getSubjectUserId() { return subjectUserId; }
	public Long getRelatedUserId() { return relatedUserId; }
	public ActivityType getType() { return type; }
	public Long getOutingId() { return outingId; }
	public Long getDebtId() { return debtId; }
	public Long getDelegationId() { return delegationId; }
	public BigDecimal getAmount() { return amount; }
	public String getDescription() { return description; }
	public Instant getCreatedAt() { return createdAt; }
}
