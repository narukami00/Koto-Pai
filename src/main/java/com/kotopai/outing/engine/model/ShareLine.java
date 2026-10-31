package com.kotopai.outing.engine.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * One consumer of an item. {@code percentage} is only used by percentage splits
 * and is {@code null} for personal and equal splits.
 */
public record ShareLine(ParticipantRef participant, BigDecimal percentage) {

    public ShareLine {
        Objects.requireNonNull(participant, "participant must not be null");
    }

    public static ShareLine of(ParticipantRef participant) {
        return new ShareLine(participant, null);
    }

    public static ShareLine of(ParticipantRef participant, String percentage) {
        return new ShareLine(participant, new BigDecimal(percentage));
    }
}
