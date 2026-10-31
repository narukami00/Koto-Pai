package com.kotopai.outing.engine.model;

import com.kotopai.common.money.Money;
import java.util.Objects;

/** Money a participant actually paid towards the bill. */
public record ContributionLine(ParticipantRef participant, Money amount) {

    public ContributionLine {
        Objects.requireNonNull(participant, "participant must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        if (amount.isNegative()) {
            throw new IllegalArgumentException("contribution must not be negative: " + amount);
        }
    }
}
