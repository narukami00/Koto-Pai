package com.kotopai.outing.engine.balance;

import com.kotopai.common.money.Money;
import com.kotopai.outing.engine.model.ParticipantRef;
import java.util.Objects;

/**
 * One participant's position in an outing: what they consumed, what they paid,
 * and the difference. Positive net means the group owes them; negative means they owe the group.
 */
public record ParticipantBalance(ParticipantRef participant, Money consumed, Money paid, Money net) {

    public ParticipantBalance {
        Objects.requireNonNull(participant, "participant must not be null");
        Objects.requireNonNull(consumed, "consumed must not be null");
        Objects.requireNonNull(paid, "paid must not be null");
        Objects.requireNonNull(net, "net must not be null");
    }

    public static ParticipantBalance of(ParticipantRef participant, Money consumed, Money paid) {
        return new ParticipantBalance(participant, consumed, paid, paid.minus(consumed));
    }
}
