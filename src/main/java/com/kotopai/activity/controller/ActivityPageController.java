package com.kotopai.activity.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.kotopai.activity.domain.ActivityType;
import com.kotopai.activity.service.ActivityService;
import com.kotopai.outing.port.CurrentUserProvider;

@Controller
public class ActivityPageController {

	private final ActivityService activities;
	private final CurrentUserProvider currentUser;

	public ActivityPageController(ActivityService activities, CurrentUserProvider currentUser) {
		this.activities = activities;
		this.currentUser = currentUser;
	}

	@GetMapping("/activity")
	public String activity(
			@RequestParam(required = false) Long friendId,
			@RequestParam(required = false) ActivityType type,
			Model model) {
		long actorId = currentUser.currentUserId();
		model.addAttribute("activities", activities.timeline(actorId, friendId, type));
		model.addAttribute("types", ActivityType.values());
		model.addAttribute("selectedFriendId", friendId);
		model.addAttribute("selectedType", type);
		return "activity/index";
	}
}
