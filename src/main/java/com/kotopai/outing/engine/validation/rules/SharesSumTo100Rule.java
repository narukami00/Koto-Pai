package com.kotopai.outing.engine.validation.rules;

import com.kotopai.outing.engine.model.ItemLine;
import com.kotopai.outing.engine.model.OutingSnapshot;
import com.kotopai.outing.engine.model.ShareLine;
import com.kotopai.outing.engine.split.SplitType;
import com.kotopai.outing.engine.validation.ValidationIssue;
import com.kotopai.outing.engine.validation.ValidationRule;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Estimated percentages on a shared item must all be present, not negative, and add up to 100. */
public final class SharesSumTo100Rule implements ValidationRule {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    @Override
    public List<ValidationIssue> check(OutingSnapshot snapshot) {
        List<ValidationIssue> issues = new ArrayList<>();
        for (ItemLine item : snapshot.items()) {
            if (item.splitType() != SplitType.PERCENTAGE || item.shares().isEmpty()) {
                continue;
            }
            BigDecimal total = BigDecimal.ZERO;
            boolean complete = true;
            for (ShareLine share : item.shares()) {
                BigDecimal pct = share.percentage();
                if (pct == null) {
                    issues.add(new ValidationIssue("PERCENTAGE_MISSING",
                        "'%s': enter a percentage for %s.".formatted(item.name(), share.participant().label())));
                    complete = false;
                } else if (pct.signum() < 0) {
                    issues.add(new ValidationIssue("PERCENTAGE_NEGATIVE",
                        "'%s': the percentage for %s cannot be negative.".formatted(item.name(), share.participant().label())));
                    complete = false;
                } else {
                    total = total.add(pct);
                }
            }
            // compareTo, not equals: 100.00 must count as 100 (see M2-03)
            if (complete && total.compareTo(HUNDRED) != 0) {
                issues.add(new ValidationIssue("PERCENTAGES_NOT_100",
                    "'%s': percentages add up to %s, not 100.".formatted(item.name(), total.stripTrailingZeros().toPlainString())));
            }
        }
        return issues;
    }
}
