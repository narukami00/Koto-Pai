package com.kotopai.outing.engine.validation;

import com.kotopai.outing.engine.model.OutingSnapshot;
import java.util.List;

/**
 * One independent check on an outing. Rules are chained by
 * {@link SnapshotValidator}: unlike a classic chain that stops at the first
 * handler that acts, every rule runs, so the user sees all problems together.
 * A rule must never throw because of bad data; it reports an issue instead.
 */
@FunctionalInterface
public interface ValidationRule {

    /** @return the problems this rule found; empty when the outing passes */
    List<ValidationIssue> check(OutingSnapshot snapshot);
}
