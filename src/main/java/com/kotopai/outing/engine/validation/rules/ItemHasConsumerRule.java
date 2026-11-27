package com.kotopai.outing.engine.validation.rules;

import com.kotopai.outing.engine.model.ItemLine;
import com.kotopai.outing.engine.model.OutingSnapshot;
import com.kotopai.outing.engine.model.ParticipantRef;
import com.kotopai.outing.engine.model.ShareLine;
import com.kotopai.outing.engine.split.SplitType;
import com.kotopai.outing.engine.validation.ValidationIssue;
import com.kotopai.outing.engine.validation.ValidationRule;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Every item needs someone to pay for it: personal items exactly one person, nobody listed twice. */
public final class ItemHasConsumerRule implements ValidationRule {

    @Override
    public List<ValidationIssue> check(OutingSnapshot snapshot) {
        List<ValidationIssue> issues = new ArrayList<>();
        for (ItemLine item : snapshot.items()) {
            if (item.shares().isEmpty()) {
                issues.add(new ValidationIssue("ITEM_NO_CONSUMER",
                    "'%s' has nobody assigned to it.".formatted(item.name())));
                continue;
            }
            if (item.splitType() == SplitType.PERSONAL && item.shares().size() > 1) {
                issues.add(new ValidationIssue("PERSONAL_ITEM_MULTIPLE_CONSUMERS",
                    "'%s' is a personal item but has %d people assigned."
                        .formatted(item.name(), item.shares().size())));
            }
            Set<ParticipantRef> seen = new HashSet<>();
            for (ShareLine share : item.shares()) {
                if (!seen.add(share.participant())) {
                    issues.add(new ValidationIssue("DUPLICATE_CONSUMER",
                        "%s is listed more than once on '%s'.".formatted(share.participant().label(), item.name())));
                }
            }
        }
        return issues;
    }
}
