package com.kotopai.common.events;

/**
 * Published when an already active outing was edited (before anyone paid). Listeners recompute what depends on the
 * numbers: the ledger regenerates debts, notifications tell participants.
 */
public record OutingUpdatedEvent(Long outingId, Long editorId) {
}
