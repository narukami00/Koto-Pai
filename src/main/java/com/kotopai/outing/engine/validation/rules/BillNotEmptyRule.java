package com.kotopai.outing.engine.validation.rules;

import com.kotopai.outing.engine.model.OutingSnapshot;
import com.kotopai.outing.engine.validation.ValidationIssue;
import com.kotopai.outing.engine.validation.ValidationRule;
import java.util.List;

/** There must be something to split: at least one item, and a bill above zero. */
public final class BillNotEmptyRule implements ValidationRule {

    @Override
    public List<ValidationIssue> check(OutingSnapshot snapshot) {
        if (snapshot.items().isEmpty()) {
            return List.of(new ValidationIssue("NO_ITEMS", "Add at least one item."));
        }
        if (!snapshot.billTotal().isPositive()) {
            return List.of(new ValidationIssue("BILL_NOT_POSITIVE", "The bill total must be more than 0."));
        }
        return List.of();
    }
}
