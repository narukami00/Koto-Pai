package com.kotopai.common.events;

public record OutingCreatedEvent(Long outingId, Long creatorId, String title) {
}
