package com.kotopai.outing.engine.validation;

import com.kotopai.outing.engine.model.OutingSnapshot;
import com.kotopai.outing.engine.validation.rules.AtLeastOneParticipantRule;
import com.kotopai.outing.engine.validation.rules.BillNotEmptyRule;
import com.kotopai.outing.engine.validation.rules.GuestOwnerPresentRule;
import com.kotopai.outing.engine.validation.rules.ItemHasConsumerRule;
import com.kotopai.outing.engine.validation.rules.KnownParticipantsRule;
import com.kotopai.outing.engine.validation.rules.PaidEqualsBillRule;
import com.kotopai.outing.engine.validation.rules.SharesSumTo100Rule;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Runs every rule against an outing and collects all issues (never stops at the first). */
public final class SnapshotValidator {

    private final List<ValidationRule> rules;

    public SnapshotValidator(List<ValidationRule> rules) {
        this.rules = List.copyOf(Objects.requireNonNull(rules, "rules must not be null"));
    }

    /** The standard rule set, in the order issues are reported. */
    public static SnapshotValidator withDefaultRules() {
        return new SnapshotValidator(List.of(
            new AtLeastOneParticipantRule(),
            new BillNotEmptyRule(),
            new KnownParticipantsRule(),
            new GuestOwnerPresentRule(),
            new ItemHasConsumerRule(),
            new SharesSumTo100Rule(),
            new PaidEqualsBillRule()));
    }

    public ValidationResult validate(OutingSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        List<ValidationIssue> issues = new ArrayList<>();
        for (ValidationRule rule : rules) {
            issues.addAll(rule.check(snapshot));
        }
        return new ValidationResult(issues);
    }

    /** @throws EngineValidationException with all issues if the outing is not valid */
    public void validateOrThrow(OutingSnapshot snapshot) {
        ValidationResult result = validate(snapshot);
        if (!result.isValid()) {
            throw new EngineValidationException(result);
        }
    }
}
