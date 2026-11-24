package com.kotopai.activity.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kotopai.activity.domain.ActivityType;
import com.kotopai.activity.service.ActivityItem;
import com.kotopai.activity.service.ActivityService;
import com.kotopai.outing.port.CurrentUserProvider;

@RestController
@RequestMapping("/api/activity")
public class ActivityRestController {

	private final ActivityService activities;
	private final CurrentUserProvider currentUser;

	public ActivityRestController(ActivityService activities, CurrentUserProvider currentUser) {
		this.activities = activities;
		this.currentUser = currentUser;
	}

	@GetMapping
	public List<ActivityItem> timeline(
			@RequestParam(required = false) Long friendId,
			@RequestParam(required = false) ActivityType type) {
		return activities.timeline(currentUser.currentUserId(), friendId, type);
	}
}
