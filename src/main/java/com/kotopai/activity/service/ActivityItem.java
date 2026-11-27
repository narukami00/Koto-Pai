package com.kotopai.activity.service;

import java.math.BigDecimal;
import java.time.Instant;

import com.kotopai.activity.domain.ActivityEvent;
import com.kotopai.activity.domain.ActivityType;

public record ActivityItem(
		long id,
		long actorId,
		Long subjectUserId,
		Long relatedUserId,
		ActivityType type,
		Long outingId,
		Long debtId,
		Long delegationId,
		BigDecimal amount,
		String description,
		Instant createdAt) {

	public static ActivityItem from(ActivityEvent event) {
		return new ActivityItem(
				event.getId(), event.getActorId(), event.getSubjectUserId(), event.getRelatedUserId(),
				event.getType(), event.getOutingId(), event.getDebtId(), event.getDelegationId(),
				event.getAmount(), event.getDescription(), event.getCreatedAt());
	}
}
