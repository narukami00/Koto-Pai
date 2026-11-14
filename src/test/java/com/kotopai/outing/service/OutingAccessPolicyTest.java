package com.kotopai.outing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kotopai.outing.domain.Outing;
import com.kotopai.outing.domain.OutingCategory;
import com.kotopai.outing.domain.OutingStatus;
import com.kotopai.outing.repository.OutingRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class OutingAccessPolicyTest {

    private static final long CREATOR = 1L;
    private static final long PARTICIPANT = 2L;
    private static final long STRANGER = 99L;

    private final OutingRepository repository = mock(OutingRepository.class);
    private final OutingAccessPolicy policy = new OutingAccessPolicy(repository);

    private static Outing outingIn(OutingStatus status) {
        Outing o = new Outing("Trip", OutingCategory.TRAVEL, LocalDate.of(2026, 11, 14), CREATOR);
        o.addUserParticipant(CREATOR);
        o.addUserParticipant(PARTICIPANT);
        switch (status) {
            case DRAFT -> { }
            case ACTIVE -> o.submit();
            case SETTLED -> {
                o.submit();
                o.markSettled();
            }
            case CANCELLED -> o.cancel();
        }
        return o;
    }

    private static long userFor(String role) {
        return switch (role) {
            case "creator" -> CREATOR;
            case "participant" -> PARTICIPANT;
            default -> STRANGER;
        };
    }

    /** The full table: every status for every kind of user. */
    @ParameterizedTest(name = "{0} outing, {1}: canView={2}, canEdit={3}")
    @CsvSource({
        "DRAFT,     creator,     true,  true",
        "DRAFT,     participant, false, false",
        "DRAFT,     stranger,    false, false",
        "ACTIVE,    creator,     true,  true",
        "ACTIVE,    participant, true,  false",
        "ACTIVE,    stranger,    false, false",
        "SETTLED,   creator,     true,  false",
        "SETTLED,   participant, true,  false",
        "SETTLED,   stranger,    false, false",
        "CANCELLED, creator,     true,  false",
        "CANCELLED, participant, true,  false",
        "CANCELLED, stranger,    false, false",
    })
    @DisplayName("Access table: status x role")
    void accessTable(OutingStatus status, String role, boolean canView, boolean canEdit) {
        Outing outing = outingIn(status);
        long user = userFor(role);

        assertThat(policy.canView(outing, user)).isEqualTo(canView);
        assertThat(policy.canEdit(outing, user)).isEqualTo(canEdit);
    }

    @Test
    @DisplayName("Only the creator is the creator; no user means no access")
    void creatorAndNullUser() {
        Outing outing = outingIn(OutingStatus.ACTIVE);

        assertThat(policy.isCreator(outing, CREATOR)).isTrue();
        assertThat(policy.isCreator(outing, PARTICIPANT)).isFalse();
        assertThat(policy.isCreator(outing, null)).isFalse();
        assertThat(policy.canView(outing, null)).isFalse();
        assertThat(policy.canEdit(outing, null)).isFalse();
    }

    @Test
    @DisplayName("A guest participant does not give any user access")
    void guestsDoNotGrantAccess() {
        Outing outing = new Outing("Trip", OutingCategory.TRAVEL, LocalDate.of(2026, 11, 14), CREATOR);
        outing.addUserParticipant(CREATOR);
        outing.submit();

        assertThat(policy.canView(outing, 10L)).isFalse();
    }

    @Test
    @DisplayName("Id-based checks look the outing up; an unknown id is simply not viewable")
    void idBasedChecks() {
        when(repository.findById(5L)).thenReturn(Optional.of(outingIn(OutingStatus.ACTIVE)));
        when(repository.findById(6L)).thenReturn(Optional.empty());

        assertThat(policy.canView(5L, PARTICIPANT)).isTrue();
        assertThat(policy.canView(5L, STRANGER)).isFalse();
        assertThat(policy.canEdit(5L, CREATOR)).isTrue();
        assertThat(policy.canEdit(5L, PARTICIPANT)).isFalse();
        assertThat(policy.canView(6L, CREATOR)).isFalse();
        assertThat(policy.canEdit(6L, CREATOR)).isFalse();
        assertThat(policy.canView((Long) null, CREATOR)).isFalse();
        assertThat(policy.canEdit((Long) null, CREATOR)).isFalse();
    }
}
