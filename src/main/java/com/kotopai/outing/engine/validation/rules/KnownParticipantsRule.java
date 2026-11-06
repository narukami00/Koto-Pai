package com.kotopai.outing.engine.validation.rules;

import com.kotopai.outing.engine.model.ContributionLine;
import com.kotopai.outing.engine.model.ItemLine;
import com.kotopai.outing.engine.model.OutingSnapshot;
import com.kotopai.outing.engine.model.ParticipantRef;
import com.kotopai.outing.engine.model.ShareLine;
import com.kotopai.outing.engine.validation.ValidationIssue;
import com.kotopai.outing.engine.validation.ValidationRule;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Everyone who consumes an item or pays must be a participant of the outing. */
public final class KnownParticipantsRule implements ValidationRule {

    @Override
    public List<ValidationIssue> check(OutingSnapshot snapshot) {
        Set<ParticipantRef> known = new HashSet<>(snapshot.participants());
        List<ValidationIssue> issues = new ArrayList<>();
        for (ItemLine item : snapshot.items()) {
            for (ShareLine share : item.shares()) {
                if (!known.contains(share.participant())) {
                    issues.add(new ValidationIssue("UNKNOWN_CONSUMER",
                        "%s is on '%s' but is not part of this outing.".formatted(share.participant().label(), item.name())));
                }
            }
        }
        for (ContributionLine payment : snapshot.contributions()) {
            if (!known.contains(payment.participant())) {
                issues.add(new ValidationIssue("UNKNOWN_PAYER",
                    "%s paid %s but is not part of this outing.".formatted(payment.participant().label(), payment.amount())));
            }
        }
        return issues;
    }
}
