package com.kotopai.outing.api.dto;

import com.kotopai.outing.domain.OutingCategory;
import com.kotopai.outing.domain.OutingStatus;
import com.kotopai.outing.engine.split.SplitType;
import com.kotopai.outing.service.OutingCommands;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO pattern: the JSON shapes of the outing API. Requests carry validation rules; responses are built from entities
 * but never expose them, so the database model and the public API can change independently and no lazy
 * association or internal field leaks into JSON.
 */
public final class OutingDtos {

    private OutingDtos() {
    }

    // ---------- requests ----------

    public record CreateOutingRequest(
        @NotBlank @Size(max = 120) String title,
        @NotNull OutingCategory category,
        @NotNull LocalDate date) {

        public OutingCommands.CreateOuting toCommand() {
            return new OutingCommands.CreateOuting(title, category, date);
        }
    }

    /** A participant: exactly one of userId or guestId (checked when converting). */
    public record ParticipantDto(Long userId, Long guestId) {

        public OutingCommands.ParticipantKey toKey() {
            return new OutingCommands.ParticipantKey(userId, guestId);
        }
    }

    public record ShareDto(
        @NotNull @Valid ParticipantDto participant,
        @DecimalMin("0.00") @DecimalMax("100.00") @Digits(integer = 3, fraction = 2) BigDecimal percentage) {
    }

    public record ItemDto(
        @NotBlank @Size(max = 120) String name,
        @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal amount,
        @NotNull SplitType splitType,
        @NotNull @Valid List<@Valid ShareDto> shares) {
    }

    public record ContributionDto(
        @NotNull @Valid ParticipantDto participant,
        @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal amount) {
    }

    /** The whole editable body of an outing; saving replaces what was there. */
    public record OutingContentsRequest(
        @NotNull @Valid List<@Valid ParticipantDto> participants,
        @NotNull @Valid List<@Valid ItemDto> items,
        @NotNull @Valid List<@Valid ContributionDto> contributions) {

        public OutingCommands.OutingContents toCommand() {
            return new OutingCommands.OutingContents(
                participants.stream().map(ParticipantDto::toKey).toList(),
                items.stream().map(item -> new OutingCommands.ItemInput(item.name(), item.amount(), item.splitType(),
                    item.shares().stream()
                        .map(s -> new OutingCommands.ShareInput(s.participant().toKey(), s.percentage())).toList())).toList(),
                contributions.stream()
                    .map(c -> new OutingCommands.ContributionInput(c.participant().toKey(), c.amount())).toList());
        }
    }

    // ---------- responses ----------

    public record OutingSummary(Long id, String title, OutingCategory category, LocalDate date, OutingStatus status,
                                Long creatorId) {
    }

    public record ParticipantView(Long id, Long userId, Long guestId, String guestName) {
    }

    public record ShareView(Long participantId, BigDecimal percentage) {
    }

    public record ItemView(Long id, String name, BigDecimal amount, SplitType splitType, List<ShareView> shares) {
    }

    public record ContributionView(Long participantId, BigDecimal amountPaid) {
    }

    public record OutingResponse(Long id, String title, OutingCategory category, LocalDate date, OutingStatus status,
                                 Long creatorId, BigDecimal billTotal, BigDecimal paidTotal,
                                 List<ParticipantView> participants, List<ItemView> items,
                                 List<ContributionView> contributions) {
    }
}
