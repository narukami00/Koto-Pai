package com.kotopai.activity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.kotopai.activity.domain.ActivityEvent;
import com.kotopai.activity.domain.ActivityType;
import com.kotopai.activity.repository.ActivityEventRepository;

@ExtendWith(MockitoExtension.class)
class ActivityServiceTest {

	@Mock ActivityEventRepository activities;

	private ActivityService service;

	@BeforeEach
	void setUp() {
		service = new ActivityService(activities);
	}

	@Test
	void filtersAuthenticatedUsersTimelineByFriendAndType() {
		ActivityEvent payment = event(10L, 1L, 2L, ActivityType.PAYMENT_RECORDED, "Payment recorded");
		ActivityEvent dispute = event(11L, 1L, 3L, ActivityType.DEBT_DISPUTED, "Debt disputed");
		when(activities.findAllByActorIdOrSubjectUserIdOrRelatedUserIdOrderByCreatedAtDesc(1L, 1L, 1L))
				.thenReturn(List.of(payment, dispute));

		assertThat(service.timeline(1L, 2L, ActivityType.PAYMENT_RECORDED))
				.singleElement()
				.satisfies(item -> {
					assertThat(item.id()).isEqualTo(10L);
					assertThat(item.subjectUserId()).isEqualTo(2L);
					assertThat(item.type()).isEqualTo(ActivityType.PAYMENT_RECORDED);
				});
	}

	@Test
	void rejectsInvalidUserAndFriendFilters() {
		assertThatThrownBy(() -> service.timeline(0L, null, null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("userId");
		assertThatThrownBy(() -> service.timeline(1L, 1L, null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("another user");
	}

	private static ActivityEvent event(
			long id,
			long actorId,
			long subjectUserId,
			ActivityType type,
			String description) {
		ActivityEvent event = new ActivityEvent(
				actorId, subjectUserId, null, type, null, null, null,
				new BigDecimal("10.00"), description);
		ReflectionTestUtils.setField(event, "id", id);
		ReflectionTestUtils.setField(event, "createdAt", Instant.parse("2026-11-18T12:00:00Z"));
		return event;
	}
}
