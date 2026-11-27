package com.kotopai.outing.service;

import com.kotopai.outing.domain.OutingCategory;
import com.kotopai.outing.engine.split.SplitType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/** Input records for {@link OutingService}: plain data from the UI or API, never entities. */
public final class OutingCommands {

    private OutingCommands() {
    }

    public record CreateOuting(String title, OutingCategory category, LocalDate date) {
    }

    /** Identifies a participant in a command: exactly one of a registered user id or a guest id. */
    public record ParticipantKey(Long userId, Long guestId) {

        public ParticipantKey {
            if ((userId == null) == (guestId == null)) {
                throw new IllegalArgumentException("a participant is exactly one of: a user id or a guest id");
            }
        }

        public static ParticipantKey user(long userId) {
            return new ParticipantKey(userId, null);
        }

        public static ParticipantKey guest(long guestId) {
            return new ParticipantKey(null, guestId);
        }

        public String label() {
            return userId != null ? "user #" + userId : "guest #" + guestId;
        }
    }

    /** A consumer of an item; the percentage is only used for percentage splits. */
    public record ShareInput(ParticipantKey participant, BigDecimal percentage) {

        public ShareInput {
            Objects.requireNonNull(participant, "participant must not be null");
        }
    }

    public record ItemInput(String name, BigDecimal amount, SplitType splitType, List<ShareInput> shares) {

        public ItemInput {
            shares = List.copyOf(shares);
        }
    }

    public record ContributionInput(ParticipantKey participant, BigDecimal amount) {

        public ContributionInput {
            Objects.requireNonNull(participant, "participant must not be null");
        }
    }

    /** The whole editable body of an outing; saving it replaces whatever was there before. */
    public record OutingContents(List<ParticipantKey> participants, List<ItemInput> items,
                                 List<ContributionInput> contributions) {

        public OutingContents {
            participants = List.copyOf(participants);
            items = List.copyOf(items);
            contributions = List.copyOf(contributions);
        }
    }
}
