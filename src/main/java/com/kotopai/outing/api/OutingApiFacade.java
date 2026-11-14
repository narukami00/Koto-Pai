package com.kotopai.outing.api;

import com.kotopai.common.money.Money;
import com.kotopai.outing.api.dto.OutingDtos.BalanceView;
import com.kotopai.outing.api.dto.OutingDtos.ContributionView;
import com.kotopai.outing.api.dto.OutingDtos.CreateOutingRequest;
import com.kotopai.outing.api.dto.OutingDtos.IssueView;
import com.kotopai.outing.api.dto.OutingDtos.ItemView;
import com.kotopai.outing.api.dto.OutingDtos.OutingContentsRequest;
import com.kotopai.outing.api.dto.OutingDtos.OutingResponse;
import com.kotopai.outing.api.dto.OutingDtos.OutingSummary;
import com.kotopai.outing.api.dto.OutingDtos.ParticipantView;
import com.kotopai.outing.api.dto.OutingDtos.PreviewResponse;
import com.kotopai.outing.api.dto.OutingDtos.ShareView;
import com.kotopai.outing.api.dto.OutingDtos.UserNetView;
import com.kotopai.outing.domain.Outing;
import com.kotopai.outing.engine.model.ParticipantRef;
import com.kotopai.outing.service.OutingPreviewService;
import com.kotopai.outing.service.OutingPreviewService.PreviewResult;
import com.kotopai.outing.service.OutingService;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs an outing use case and converts the result to DTOs inside the same transaction. Open-session-in-view is off
 * (deliberately), so entities cannot be read lazily by controllers; converting here keeps controllers thin and
 * means no entity ever reaches the JSON layer.
 */
@Service
@Transactional
public class OutingApiFacade {

    static final int MAX_PAGE = 100;

    private final OutingService outings;
    private final OutingPreviewService previews;

    public OutingApiFacade(OutingService outings, OutingPreviewService previews) {
        this.outings = Objects.requireNonNull(outings, "outings must not be null");
        this.previews = Objects.requireNonNull(previews, "previews must not be null");
    }

    public OutingResponse create(Long actorId, CreateOutingRequest request) {
        return toResponse(outings.create(actorId, request.toCommand()));
    }

    @Transactional(readOnly = true)
    public List<OutingSummary> list(Long actorId, int limit) {
        int size = Math.min(Math.max(limit, 1), MAX_PAGE);
        return outings.listVisibleTo(actorId, PageRequest.of(0, size)).stream().map(OutingApiFacade::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public OutingResponse get(Long actorId, Long outingId) {
        return toResponse(outings.view(actorId, outingId));
    }

    public OutingResponse updateContents(Long actorId, Long outingId, OutingContentsRequest request) {
        return toResponse(outings.updateContents(actorId, outingId, request.toCommand()));
    }

    public OutingResponse submit(Long actorId, Long outingId) {
        return toResponse(outings.submit(actorId, outingId));
    }

    public OutingResponse cancel(Long actorId, Long outingId) {
        return toResponse(outings.cancel(actorId, outingId));
    }

    /** Dry run: calculates unsaved input and returns the numbers, or every problem if it is incomplete. */
    @Transactional(readOnly = true)
    public PreviewResponse preview(Long actorId, OutingContentsRequest request) {
        return toPreviewResponse(previews.preview(actorId, request.toCommand()));
    }

    public void delete(Long actorId, Long outingId) {
        outings.delete(actorId, outingId);
    }

    static PreviewResponse toPreviewResponse(PreviewResult preview) {
        List<IssueView> issues = preview.issues().stream().map(i -> new IssueView(i.code(), i.message())).toList();
        if (!preview.isValid()) {
            return new PreviewResponse(false, issues, preview.billTotal().amount(), preview.paidTotal().amount(),
                List.of(), List.of());
        }
        List<BalanceView> balances = preview.result().balances().stream().map(b -> {
            Long userId = b.participant() instanceof ParticipantRef.User u ? u.userId() : null;
            Long guestId = b.participant() instanceof ParticipantRef.Guest g ? g.guestId() : null;
            return new BalanceView(userId, guestId, b.consumed().amount(), b.paid().amount(), b.net().amount());
        }).toList();
        List<UserNetView> userNets = preview.result().folded().users().stream()
            .map(u -> new UserNetView(u.userId(), u.net().amount())).toList();
        return new PreviewResponse(true, issues, preview.billTotal().amount(), preview.paidTotal().amount(), balances, userNets);
    }

    static OutingSummary toSummary(Outing o) {
        return new OutingSummary(o.getId(), o.getTitle(), o.getCategory(), o.getDate(), o.getStatus(), o.getCreatorId());
    }

    static OutingResponse toResponse(Outing o) {
        List<ParticipantView> participants = o.getParticipants().stream()
            .map(p -> new ParticipantView(p.getId(), p.getUserId(),
                p.isGuest() ? p.getGuest().getId() : null, p.isGuest() ? p.getGuest().getName() : null))
            .toList();
        List<ItemView> items = o.getItems().stream()
            .map(i -> new ItemView(i.getId(), i.getName(), i.getAmount(), i.getSplitType(),
                i.getShares().stream().map(s -> new ShareView(s.getParticipant().getId(), s.getPercentage())).toList()))
            .toList();
        List<ContributionView> contributions = o.getContributions().stream()
            .map(c -> new ContributionView(c.getParticipant().getId(), c.getAmountPaid()))
            .toList();
        Money bill = Money.sum(o.getItems().stream().map(i -> Money.of(i.getAmount())).toList());
        Money paid = Money.sum(o.getContributions().stream().map(c -> Money.of(c.getAmountPaid())).toList());
        return new OutingResponse(o.getId(), o.getTitle(), o.getCategory(), o.getDate(), o.getStatus(), o.getCreatorId(),
            bill.amount(), paid.amount(), participants, items, contributions);
    }
}
