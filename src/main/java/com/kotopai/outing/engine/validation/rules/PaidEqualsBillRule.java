package com.kotopai.outing.engine.validation.rules;

import com.kotopai.common.money.Money;
import com.kotopai.outing.engine.model.OutingSnapshot;
import com.kotopai.outing.engine.validation.ValidationIssue;
import com.kotopai.outing.engine.validation.ValidationRule;
import java.util.List;

/** What people actually paid must equal the bill, otherwise someone's net is wrong. */
public final class PaidEqualsBillRule implements ValidationRule {

    @Override
    public List<ValidationIssue> check(OutingSnapshot snapshot) {
        Money bill = snapshot.billTotal();
        Money paid = snapshot.paidTotal();
        if (paid.equals(bill)) {
            return List.of();
        }
        Money difference = paid.minus(bill);
        String gap = difference.isNegative()
            ? "short by " + difference.abs()
            : "over by " + difference;
        return List.of(new ValidationIssue("PAID_NOT_EQUAL_BILL",
            "Payments total %s but the bill is %s (%s).".formatted(paid, bill, gap)));
    }
}
