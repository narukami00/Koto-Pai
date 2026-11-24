package com.kotopai.activity.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kotopai.activity.domain.ActivityEvent;
import com.kotopai.activity.domain.ActivityType;
import com.kotopai.activity.repository.ActivityEventRepository;

@Service
public class ActivityService {

	private final ActivityEventRepository activities;

	public ActivityService(ActivityEventRepository activities) {
		this.activities = activities;
	}

	@Transactional(readOnly = true)
	public List<ActivityItem> timeline(long userId, Long friendId, ActivityType type) {
		if (userId <= 0) {
			throw new IllegalArgumentException("userId must be positive");
		}
		if (friendId != null && (friendId <= 0 || friendId == userId)) {
			throw new IllegalArgumentException("friendId must identify another user");
		}
		return activities.findAllByActorIdOrSubjectUserIdOrRelatedUserIdOrderByCreatedAtDesc(
				userId, userId, userId).stream()
				.filter(event -> type == null || event.getType() == type)
				.filter(event -> friendId == null || involves(event, friendId))
				.map(ActivityItem::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<ActivityItem> between(long userId, long friendId) {
		return timeline(userId, friendId, null);
	}

	private static boolean involves(ActivityEvent event, long userId) {
		return event.getActorId() == userId
				|| (event.getSubjectUserId() != null && event.getSubjectUserId() == userId)
				|| (event.getRelatedUserId() != null && event.getRelatedUserId() == userId);
	}
}
