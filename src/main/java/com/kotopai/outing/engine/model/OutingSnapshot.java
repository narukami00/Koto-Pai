package com.kotopai.outing.engine.model;

import com.kotopai.common.money.Money;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Everything the split engine needs about one outing: who took part, what
 * was consumed, and who paid. Immutable; build it with {@link #builder()}.
 *
 * <p>The snapshot only guards its structure (no nulls, no duplicate
 * participants). Business rules such as "paid must equal the bill" are
 * checked by the engine's validator so that all problems are reported together.
 */
public record OutingSnapshot(
    List<ParticipantRef> participants,
    List<ItemLine> items,
    List<ContributionLine> contributions) {

    public OutingSnapshot {
        participants = List.copyOf(participants);
        items = List.copyOf(items);
        contributions = List.copyOf(contributions);
        if (new LinkedHashSet<>(participants).size() != participants.size()) {
            throw new IllegalArgumentException("participants must not contain duplicates: " + participants);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Sum of all item amounts. */
    public Money billTotal() {
        return Money.sum(items.stream().map(ItemLine::amount).toList());
    }

    /** Sum of all contributions. */
    public Money paidTotal() {
        return Money.sum(contributions.stream().map(ContributionLine::amount).toList());
    }

    /**
     * Builder pattern: assembles a snapshot step by step in readable form,
     * e.g. {@code builder().participants(a, b).personalItem("Burger", "200", a).paid(a, "200").build()}.
     */
    public static final class Builder {

        private final Set<ParticipantRef> participants = new LinkedHashSet<>();
        private final List<ItemLine> items = new ArrayList<>();
        private final List<ContributionLine> contributions = new ArrayList<>();

        private Builder() {
        }

        public Builder participant(ParticipantRef participant) {
            Objects.requireNonNull(participant, "participant must not be null");
            if (!participants.add(participant)) {
                throw new IllegalArgumentException("participant added twice: " + participant);
            }
            return this;
        }

        public Builder participants(ParticipantRef... participants) {
            for (ParticipantRef p : participants) {
                participant(p);
            }
            return this;
        }

        public Builder item(ItemLine item) {
            items.add(Objects.requireNonNull(item, "item must not be null"));
            return this;
        }

        public Builder personalItem(String name, String amount, ParticipantRef consumer) {
            return item(ItemLine.personal(name, Money.of(amount), consumer));
        }

        public Builder equalItem(String name, String amount, ParticipantRef... consumers) {
            return item(ItemLine.equal(name, Money.of(amount), consumers));
        }

        public Builder percentageItem(String name, String amount, ShareLine... shares) {
            return item(ItemLine.percentage(name, Money.of(amount), shares));
        }

        public Builder paid(ParticipantRef participant, Money amount) {
            contributions.add(new ContributionLine(participant, amount));
            return this;
        }

        public Builder paid(ParticipantRef participant, String amount) {
            return paid(participant, Money.of(amount));
        }

        /** Creates an immutable snapshot; the builder can keep being used afterwards. */
        public OutingSnapshot build() {
            return new OutingSnapshot(List.copyOf(participants), items, contributions);
        }
    }
}
