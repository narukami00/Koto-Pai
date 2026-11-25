package com.kotopai.outing.controller;

import com.kotopai.common.money.Money;
import com.kotopai.outing.domain.Item;
import com.kotopai.outing.domain.Outing;
import com.kotopai.outing.domain.OutingCategory;
import com.kotopai.outing.domain.OutingParticipant;
import com.kotopai.outing.domain.OutingStatus;
import com.kotopai.outing.engine.EngineResult;
import com.kotopai.outing.engine.SplitEngine;
import com.kotopai.outing.engine.balance.ParticipantBalance;
import com.kotopai.outing.engine.validation.EngineValidationException;
import com.kotopai.outing.mapping.OutingSnapshotMapper;
import com.kotopai.outing.service.OutingAccessPolicy;
import com.kotopai.outing.service.OutingService;
import com.kotopai.user.dto.UserSummary;
import com.kotopai.user.service.UserService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds the view models for the outing pages inside one read-only transaction (open-session-in-view is off), so
 * templates only ever see plain records: display names, formatted shares and the engine numbers per person.
 */
@Service
@Transactional(readOnly = true)
public class OutingPageFacade {

    static final int LIST_LIMIT = 100;

    // ---------- view models ----------

    public record OutingRow(Long id, String title, OutingCategory category, LocalDate date, OutingStatus status,
                            String creatorName, boolean mine, BigDecimal billTotal) {
    }

    public record OutingList(List<OutingRow> active, List<OutingRow> drafts, List<OutingRow> past) {
        public boolean isEmpty() {
            return active.isEmpty() && drafts.isEmpty() && past.isEmpty();
        }
    }

    public record ItemRow(String name, BigDecimal amount, String splitLabel, String sharedBy) {
    }

    /** One person on the outing. The money fields are null while the outing cannot be calculated yet. */
    public record PersonRow(String name, String initial, String avatarColor, boolean guest, String subtitle,
                            BigDecimal consumed, BigDecimal paid, BigDecimal net) {
        public boolean hasNumbers() {
            return net != null;
        }
    }

    public record OutingDetail(Long id, String title, OutingCategory category, LocalDate date, OutingStatus status,
                               String creatorName, boolean mine, boolean canEdit, boolean canSubmit, boolean canCancel, boolean canDelete,
                               BigDecimal billTotal, BigDecimal paidTotal, List<ItemRow> items, List<PersonRow> people,
                               List<String> issues) {
        public boolean isCalculated() {
            return issues.isEmpty();
        }
    }

    private final OutingService outings;
    private final OutingSnapshotMapper mapper;
    private final SplitEngine engine;
    private final UserService users;
    private final OutingAccessPolicy access;

    public OutingPageFacade(OutingService outings, OutingSnapshotMapper mapper, SplitEngine engine, UserService users,
                            OutingAccessPolicy access) {
        this.outings = Objects.requireNonNull(outings);
        this.mapper = Objects.requireNonNull(mapper);
        this.engine = Objects.requireNonNull(engine);
        this.users = Objects.requireNonNull(users);
        this.access = Objects.requireNonNull(access);
    }

    public OutingList list(Long actorId) {
        List<Outing> visible = outings.listVisibleTo(actorId, PageRequest.of(0, LIST_LIMIT));
        Map<Long, UserSummary> creators = users.summariesOf(
            visible.stream().map(Outing::getCreatorId).collect(Collectors.toSet()));
        List<OutingRow> active = new ArrayList<>();
        List<OutingRow> drafts = new ArrayList<>();
        List<OutingRow> past = new ArrayList<>();
        for (Outing o : visible) {
            OutingRow row = new OutingRow(o.getId(), o.getTitle(), o.getCategory(), o.getDate(), o.getStatus(),
                nameOf(creators, o.getCreatorId()), o.getCreatorId().equals(actorId), billOf(o).amount());
            switch (o.getStatus()) {
                case ACTIVE -> active.add(row);
                case DRAFT -> drafts.add(row);
                default -> past.add(row);
            }
        }
        return new OutingList(active, drafts, past);
    }

    public OutingDetail detail(Long actorId, Long outingId) {
        Outing outing = outings.view(actorId, outingId);
        Set<Long> userIds = new HashSet<>();
        userIds.add(outing.getCreatorId());
        outing.getParticipants().forEach(p -> {
            if (p.getUserId() != null) {
                userIds.add(p.getUserId());
            }
            if (p.isGuest()) {
                userIds.add(p.getGuest().getOwnerId());
            }
        });
        Map<Long, UserSummary> names = users.summariesOf(userIds);

        List<String> issues = new ArrayList<>();
        EngineResult result = null;
        try {
            result = engine.calculate(mapper.toSnapshot(outing));
        } catch (EngineValidationException e) {
            e.getResult().issues().forEach(i -> issues.add(i.message()));
        }

        Map<Long, String> participantNames = new HashMap<>();
        List<PersonRow> people = new ArrayList<>();
        List<OutingParticipant> participants = outing.getParticipants();
        for (int i = 0; i < participants.size(); i++) {
            OutingParticipant p = participants.get(i);
            String name = p.isGuest() ? p.getGuest().getName() : nameOf(names, p.getUserId());
            participantNames.put(p.getId(), name);
            ParticipantBalance balance = result == null ? null : result.balances().get(i);
            people.add(personRow(p, name, names, balance));
        }

        List<ItemRow> items = outing.getItems().stream().map(item -> itemRow(item, participantNames)).toList();
        boolean mine = access.isCreator(outing, actorId);
        OutingStatus status = outing.getStatus();
        return new OutingDetail(outing.getId(), outing.getTitle(), outing.getCategory(), outing.getDate(), status,
            nameOf(names, outing.getCreatorId()), mine,
            mine && (status == OutingStatus.DRAFT || status == OutingStatus.ACTIVE),
            mine && status == OutingStatus.DRAFT,
            mine && (status == OutingStatus.DRAFT || status == OutingStatus.ACTIVE),
            mine && status == OutingStatus.DRAFT,
            billOf(outing).amount(), paidOf(outing).amount(), items, people, issues);
    }

    private static PersonRow personRow(OutingParticipant p, String name, Map<Long, UserSummary> names, ParticipantBalance b) {
        String initial = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase();
        String color = p.isGuest() ? null : (names.containsKey(p.getUserId()) ? names.get(p.getUserId()).avatarColor() : null);
        String subtitle = p.isGuest()
            ? "Guest of " + nameOf(names, p.getGuest().getOwnerId())
            : (names.containsKey(p.getUserId()) ? "@" + names.get(p.getUserId()).userCode() : "");
        return new PersonRow(name, initial, color, p.isGuest(), subtitle,
            b == null ? null : b.consumed().amount(), b == null ? null : b.paid().amount(), b == null ? null : b.net().amount());
    }

    private static ItemRow itemRow(Item item, Map<Long, String> participantNames) {
        String sharedBy = switch (item.getSplitType()) {
            case PERCENTAGE -> item.getShares().stream()
                .map(s -> participantNames.get(s.getParticipant().getId()) + " " + trimZeros(s.getPercentage()) + "%")
                .collect(Collectors.joining(", "));
            default -> item.getShares().stream()
                .map(s -> participantNames.get(s.getParticipant().getId())).collect(Collectors.joining(", "));
        };
        String label = switch (item.getSplitType()) {
            case PERSONAL -> "Personal";
            case EQUAL -> "Split equally";
            case PERCENTAGE -> "By percentage";
        };
        return new ItemRow(item.getName(), item.getAmount(), label, sharedBy);
    }

    private static String trimZeros(BigDecimal value) {
        return value == null ? "?" : value.stripTrailingZeros().toPlainString();
    }

    private static String nameOf(Map<Long, UserSummary> names, Long userId) {
        UserSummary summary = names.get(userId);
        return summary == null ? "User #" + userId : summary.displayName();
    }

    private static Money billOf(Outing o) {
        return Money.sum(o.getItems().stream().map(i -> Money.of(i.getAmount())).toList());
    }

    private static Money paidOf(Outing o) {
        return Money.sum(o.getContributions().stream().map(c -> Money.of(c.getAmountPaid())).toList());
    }
}
