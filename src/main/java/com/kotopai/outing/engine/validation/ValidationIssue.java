package com.kotopai.outing.engine.validation;

import java.util.Objects;

/** One problem found in an outing: a stable machine-readable code plus a message for people. */
public record ValidationIssue(String code, String message) {

    public ValidationIssue {
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(message, "message must not be null");
    }
}
