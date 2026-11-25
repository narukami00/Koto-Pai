package com.kotopai.outing.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kotopai.outing.controller.OutingPageFacade.OutingDetail;
import com.kotopai.outing.controller.OutingPageFacade.OutingList;
import com.kotopai.outing.controller.OutingPageFacade.PersonRow;
import com.kotopai.outing.domain.Guest;
import com.kotopai.outing.domain.Item;
import com.kotopai.outing.domain.Outing;
import com.kotopai.outing.domain.OutingCategory;
import com.kotopai.outing.domain.OutingParticipant;
import com.kotopai.outing.engine.SplitEngine;
import com.kotopai.outing.engine.split.SplitType;
import com.kotopai.outing.mapping.OutingSnapshotMapper;
import com.kotopai.outing.service.OutingAccessPolicy;
import com.kotopai.outing.service.OutingService;
import com.kotopai.user.dto.UserSummary;
import com.kotopai.user.service.UserService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class OutingPageFacadeTest {

    private final OutingService outings = mock(OutingService.class);
    private final UserService users = mock(UserService.class);
    private final OutingAccessPolicy access = mock(OutingAccessPolicy.class);
    private final OutingPageFacade facade =
        new OutingPageFacade(outings, new OutingSnapshotMapper(), SplitEngine.withDefaults(), users, access);

    private static void id(Object entity, long id) {
        ReflectionTestUtils.setField(entity, "id", id);
    }

    private static Outing outing(long id, String title, long creator) {
        Outing o = new Outing(title, OutingCategory.EATING, LocalDate.of(2026, 11, 24), creator);
        id(o, id);
        return o;
    }

    /** Burger night with Rafsan's guest Rahim: Rafsan 1, Tanvir 2. */
    private Outing burgerNightWithGuest() {
        Outing o = outing(7, "Burger night", 1);
        Guest rahim = new Guest(1L, "Rahim");
        id(rahim, 10);
        OutingParticipant rafsan = o.addUserParticipant(1L);
        OutingParticipant tanvir = o.addUserParticipant(2L);
        OutingParticipant guest = o.addGuestParticipant(rahim);
        long next = 100;
        for (OutingParticipant p : List.of(rafsan, tanvir, guest)) {
            id(p, next++);
        }
        Item pizza = o.addItem("Large pizza", new BigDecimal("1000.00"), SplitType.PERCENTAGE);
        pizza.addShare(rafsan, new BigDecimal("50"));
        pizza.addShare(tanvir, new BigDecimal("30"));
        pizza.addShare(guest, new BigDecimal("20"));
        Item shake = o.addItem("Shake", new BigDecimal("150.00"), SplitType.PERSONAL);
        shake.addShare(guest, null);
        o.addContribution(rafsan, new BigDecimal("1150.00"));
        when(users.summariesOf(any())).thenReturn(Map.of(
            1L, new UserSummary(1L, "rafsan10", "Rafsan", "#18604B"),
            2L, new UserSummary(2L, "tanvir_h", "Tanvir", null)));
        return o;
    }

    @Test
    @DisplayName("The list groups outings by status and names the creator")
    void listGroupsByStatus() {
        Outing active = outing(1, "Active one", 1);
        active.submit();
        Outing draft = outing(2, "Draft one", 1);
        Outing cancelled = outing(3, "Cancelled one", 2);
        cancelled.cancel();
        when(outings.listVisibleTo(any(), any())).thenReturn(List.of(active, draft, cancelled));
        when(users.summariesOf(any())).thenReturn(Map.of(2L, new UserSummary(2L, "tanvir_h", "Tanvir", null)));

        OutingList list = facade.list(1L);

        assertThat(list.active()).extracting(r -> r.title()).containsExactly("Active one");
        assertThat(list.drafts()).extracting(r -> r.title()).containsExactly("Draft one");
        assertThat(list.past()).extracting(r -> r.title()).containsExactly("Cancelled one");
        assertThat(list.past().get(0).creatorName()).isEqualTo("Tanvir");
        assertThat(list.past().get(0).mine()).isFalse();
        assertThat(list.active().get(0).mine()).isTrue();
        assertThat(list.isEmpty()).isFalse();
    }

    @Test
    @DisplayName("An empty list is reported as empty")
    void emptyList() {
        when(outings.listVisibleTo(any(), any())).thenReturn(List.of());
        when(users.summariesOf(any())).thenReturn(Map.of());

        assertThat(facade.list(1L).isEmpty()).isTrue();
    }

    @Test
    @DisplayName("A complete outing shows names, shares and each person ate / paid / net")
    void calculatedDetail() {
        Outing o = burgerNightWithGuest();
        when(outings.view(1L, 7L)).thenReturn(o);
        when(access.isCreator(o, 1L)).thenReturn(true);

        OutingDetail detail = facade.detail(1L, 7L);

        assertThat(detail.isCalculated()).isTrue();
        assertThat(detail.billTotal()).isEqualByComparingTo("1150.00");
        assertThat(detail.people()).extracting(PersonRow::name).containsExactly("Rafsan", "Tanvir", "Rahim");
        PersonRow rafsan = detail.people().get(0);
        assertThat(rafsan.consumed()).isEqualByComparingTo("500.00");
        assertThat(rafsan.paid()).isEqualByComparingTo("1150.00");
        assertThat(rafsan.net()).isEqualByComparingTo("650.00");
        assertThat(rafsan.subtitle()).isEqualTo("@rafsan10");
        PersonRow rahim = detail.people().get(2);
        assertThat(rahim.guest()).isTrue();
        assertThat(rahim.subtitle()).isEqualTo("Guest of Rafsan");
        assertThat(rahim.net()).isEqualByComparingTo("-350.00");
        assertThat(detail.items().get(0).sharedBy()).isEqualTo("Rafsan 50%, Tanvir 30%, Rahim 20%");
        assertThat(detail.items().get(0).splitLabel()).isEqualTo("By percentage");
        assertThat(detail.items().get(1).sharedBy()).isEqualTo("Rahim");
    }

    @Test
    @DisplayName("An incomplete draft lists the problems and has no money numbers; the creator can delete it")
    void incompleteDraft() {
        Outing o = outing(8, "Half done", 1);
        OutingParticipant rafsan = o.addUserParticipant(1L);
        id(rafsan, 200);
        o.addItem("Burger", new BigDecimal("200.00"), SplitType.PERSONAL).addShare(rafsan, null);
        when(outings.view(1L, 8L)).thenReturn(o);
        when(access.isCreator(o, 1L)).thenReturn(true);
        when(users.summariesOf(any())).thenReturn(Map.of());

        OutingDetail detail = facade.detail(1L, 8L);

        assertThat(detail.isCalculated()).isFalse();
        assertThat(detail.issues()).anyMatch(m -> m.contains("short by 200.00"));
        assertThat(detail.people().get(0).hasNumbers()).isFalse();
        assertThat(detail.people().get(0).name()).isEqualTo("User #1");
        assertThat(detail.canEdit()).isTrue();
        assertThat(detail.canSubmit()).isTrue();
        assertThat(detail.canDelete()).isTrue();
        assertThat(detail.canCancel()).isTrue();
    }

    @Test
    @DisplayName("A participant who is not the creator gets no action buttons; cancelled outings offer none")
    void actionAvailability() {
        Outing o = burgerNightWithGuest();
        o.submit();
        when(outings.view(2L, 7L)).thenReturn(o);
        when(access.isCreator(o, 2L)).thenReturn(false);

        OutingDetail participantView = facade.detail(2L, 7L);

        assertThat(participantView.mine()).isFalse();
        assertThat(participantView.canEdit()).isFalse();
        assertThat(participantView.canSubmit()).isFalse();
        assertThat(participantView.canCancel()).isFalse();
        assertThat(participantView.canDelete()).isFalse();

        o.cancel();
        when(outings.view(1L, 7L)).thenReturn(o);
        when(access.isCreator(o, 1L)).thenReturn(true);
        OutingDetail cancelled = facade.detail(1L, 7L);
        assertThat(cancelled.canEdit()).isFalse();
        assertThat(cancelled.canCancel()).isFalse();
        assertThat(cancelled.canSubmit()).isFalse();
    }
}
