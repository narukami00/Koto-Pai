package com.kotopai.outing.engine.validation;

import java.util.Objects;

/** Thrown when an outing fails validation; carries every issue found. */
public class EngineValidationException extends RuntimeException {

    private final transient ValidationResult result;

    public EngineValidationException(ValidationResult result) {
        super("Invalid outing: " + String.join("; ", Objects.requireNonNull(result, "result must not be null").messages()));
        this.result = result;
    }

    public ValidationResult getResult() {
        return result;
    }
}
