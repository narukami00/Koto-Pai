package com.kotopai.ledger.event;

/** Type-safe ledger integration event published when an outing becomes active. */
public record OutingActivatedEvent(Long outingId, Long creatorId) {
}
