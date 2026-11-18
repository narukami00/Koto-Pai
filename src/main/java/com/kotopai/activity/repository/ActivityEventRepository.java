package com.kotopai.activity.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kotopai.activity.domain.ActivityEvent;

public interface ActivityEventRepository extends JpaRepository<ActivityEvent, Long> {

	List<ActivityEvent> findAllByActorIdOrSubjectUserIdOrRelatedUserIdOrderByCreatedAtDesc(
			Long actorId, Long subjectUserId, Long relatedUserId);
}
