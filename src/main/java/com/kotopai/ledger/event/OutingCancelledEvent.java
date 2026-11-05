package com.kotopai.ledger.event;

/** Type-safe ledger integration event published when an outing is cancelled. */
public record OutingCancelledEvent(Long outingId) {
}
