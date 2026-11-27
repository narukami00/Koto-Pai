package com.kotopai.outing.service;

import com.kotopai.outing.domain.Outing;
import com.kotopai.outing.domain.OutingStatus;
import com.kotopai.outing.repository.OutingRepository;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Who may see or change an outing, in one place instead of scattered checks in services and controllers.
 * Registered as {@code outingAccess} so it can be used in expressions such as
 * {@code @PreAuthorize("@outingAccess.canView(#id, principal.id)")}.
 *
 * <ul>
 *   <li>view: the creator always; a participant once the outing is no longer a draft; nobody else</li>
 *   <li>edit: the creator only, and only in a status that allows editing (draft or active; the payment lock is a
 *       separate check made against the ledger)</li>
 * </ul>
 */
@Component("outingAccess")
public class OutingAccessPolicy {

    private final OutingRepository outings;

    public OutingAccessPolicy(OutingRepository outings) {
        this.outings = Objects.requireNonNull(outings, "outings must not be null");
    }

    public boolean isCreator(Outing outing, Long userId) {
        return userId != null && userId.equals(outing.getCreatorId());
    }

    public boolean canView(Outing outing, Long userId) {
        if (userId == null) {
            return false;
        }
        if (isCreator(outing, userId)) {
            return true;
        }
        return outing.getStatus() != OutingStatus.DRAFT
            && outing.getParticipants().stream().anyMatch(p -> userId.equals(p.getUserId()));
    }

    public boolean canEdit(Outing outing, Long userId) {
        return isCreator(outing, userId) && outing.getStatus().canEdit(false);
    }

    /** Id-based form for security expressions; an unknown outing is simply not viewable. */
    @Transactional(readOnly = true)
    public boolean canView(Long outingId, Long userId) {
        return outingId != null && outings.findById(outingId).map(o -> canView(o, userId)).orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean canEdit(Long outingId, Long userId) {
        return outingId != null && outings.findById(outingId).map(o -> canEdit(o, userId)).orElse(false);
    }
}
