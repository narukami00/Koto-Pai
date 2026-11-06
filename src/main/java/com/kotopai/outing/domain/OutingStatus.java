package com.kotopai.outing.domain;

/** Lifecycle of an outing; legal transitions are enforced by the State pattern in M2-15. */
public enum OutingStatus {
    DRAFT, ACTIVE, SETTLED, CANCELLED
}
