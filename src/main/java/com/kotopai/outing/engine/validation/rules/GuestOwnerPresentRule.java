package com.kotopai.outing.engine.validation.rules;

import com.kotopai.outing.engine.model.OutingSnapshot;
import com.kotopai.outing.engine.model.ParticipantRef;
import com.kotopai.outing.engine.validation.ValidationIssue;
import com.kotopai.outing.engine.validation.ValidationRule;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A guest's balance is folded into the registered user who owns the guest, so
 * that owner must be on the outing too (guest proxy model).
 */
public final class GuestOwnerPresentRule implements ValidationRule {

    @Override
    public List<ValidationIssue> check(OutingSnapshot snapshot) {
        Set<ParticipantRef> known = new HashSet<>(snapshot.participants());
        List<ValidationIssue> issues = new ArrayList<>();
        for (ParticipantRef participant : snapshot.participants()) {
            if (participant instanceof ParticipantRef.Guest guest
                && !known.contains(ParticipantRef.user(guest.ownerUserId()))) {
                issues.add(new ValidationIssue("GUEST_OWNER_MISSING",
                    "%s belongs to user #%d, who is not part of this outing."
                        .formatted(guest.label(), guest.ownerUserId())));
            }
        }
        return issues;
    }
}
