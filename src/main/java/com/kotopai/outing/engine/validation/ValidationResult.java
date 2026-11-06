package com.kotopai.outing.engine.validation;

import java.util.List;

/** Everything wrong with an outing, collected in one go. Valid when there are no issues. */
public record ValidationResult(List<ValidationIssue> issues) {

    public ValidationResult {
        issues = List.copyOf(issues);
    }

    public boolean isValid() {
        return issues.isEmpty();
    }

    public List<String> messages() {
        return issues.stream().map(ValidationIssue::message).toList();
    }

    public List<String> codes() {
        return issues.stream().map(ValidationIssue::code).toList();
    }
}
