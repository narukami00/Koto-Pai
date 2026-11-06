package com.kotopai.outing.engine.validation.rules;

import com.kotopai.outing.engine.model.OutingSnapshot;
import com.kotopai.outing.engine.validation.ValidationIssue;
import com.kotopai.outing.engine.validation.ValidationRule;
import java.util.List;

/** An outing needs at least one person. */
public final class AtLeastOneParticipantRule implements ValidationRule {

    @Override
    public List<ValidationIssue> check(OutingSnapshot snapshot) {
        if (snapshot.participants().isEmpty()) {
            return List.of(new ValidationIssue("NO_PARTICIPANTS", "Add at least one participant."));
        }
        return List.of();
    }
}
