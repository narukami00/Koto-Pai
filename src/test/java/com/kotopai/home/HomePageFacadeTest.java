package com.kotopai.home;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kotopai.home.HomePageFacade.HomeView;
import com.kotopai.ledger.service.CounterpartyType;
import com.kotopai.ledger.service.LedgerBalance;
import com.kotopai.ledger.service.LedgerService;
import com.kotopai.ledger.service.LedgerSummary;
import com.kotopai.outing.controller.OutingPageFacade;
import com.kotopai.outing.controller.OutingPageFacade.OutingList;
import com.kotopai.outing.controller.OutingPageFacade.OutingRow;
import com.kotopai.outing.domain.Guest;
import com.kotopai.outing.domain.OutingCategory;
import com.kotopai.outing.domain.OutingStatus;
import com.kotopai.outing.repository.GuestRepository;
import com.kotopai.user.dto.UserSummary;
import com.kotopai.user.service.UserService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class HomePageFacadeTest {

    private final LedgerService ledger = mock(LedgerService.class);
    private final OutingPageFacade outings = mock(OutingPageFacade.class);
    private final UserService users = mock(UserService.class);
    private final GuestRepository guests = mock(GuestRepository.class);
    private final HomePageFacade facade = new HomePageFacade(ledger, outings, users, guests);

    private static BigDecimal bdt(String value) {
        return new BigDecimal(value);
    }

    private static LedgerBalance balance(long id, CounterpartyType type, String amount) {
        return new LedgerBalance(id, type, bdt(amount));
    }

    private static OutingRow row(long id, OutingStatus status) {
        return new OutingRow(id, "Outing " + id, OutingCategory.EATING, LocalDate.of(2026, 11, 24), status,
            "Rafsan", true, bdt("100.00"));
    }

    private void givenLedger(List<LedgerBalance> balances) {
        BigDecimal receivable = BigDecimal.ZERO.setScale(2);
        BigDecimal payable = BigDecimal.ZERO.setScale(2);
        for (LedgerBalance b : balances) {
            if (b.balance().signum() > 0) {
                receivable = receivable.add(b.balance());
            } else {
                payable = payable.add(b.balance().abs());
            }
        }
        when(ledger.summaryFor(1L)).thenReturn(new LedgerSummary(1L, receivable, payable,
            receivable.subtract(payable), balances));
    }

    private void givenNoOutings() {
        when(outings.list(1L)).thenReturn(new OutingList(List.of(), List.of(), List.of()));
    }

    @Test
    @DisplayName("A user with nothing owed is settled up and sees empty lists")
    void settledUp() {
        when(ledger.summaryFor(1L)).thenReturn(LedgerSummary.empty(1L));
        givenNoOutings();
        when(users.summariesOf(any())).thenReturn(Map.of());

        HomeView view = facade.forUser(1L, "Rafsan");

        assertThat(view.isSettledUp()).isTrue();
        assertThat(view.balances()).isEmpty();
        assertThat(view.recentOutings()).isEmpty();
        assertThat(view.net()).isEqualByComparingTo("0");
        assertThat(view.displayName()).isEqualTo("Rafsan");
    }

    @Test
    @DisplayName("Totals come from the ledger and balances are named and signed")
    void namesAndSignsBalances() {
        givenLedger(List.of(balance(2, CounterpartyType.USER, "300.00"), balance(3, CounterpartyType.USER, "-50.00"),
            balance(10, CounterpartyType.GUEST, "120.00")));
        givenNoOutings();
        when(users.summariesOf(any())).thenReturn(Map.of(
            2L, new UserSummary(2L, "tanvir_h", "Tanvir", null),
            3L, new UserSummary(3L, "nabila_r", "Nabila", null)));
        Guest rahim = new Guest(1L, "Rahim");
        ReflectionTestUtils.setField(rahim, "id", 10L);
        when(guests.findAllById(any())).thenReturn(List.of(rahim));

        HomeView view = facade.forUser(1L, "Rafsan");

        assertThat(view.net()).isEqualByComparingTo("370.00");
        assertThat(view.receivable()).isEqualByComparingTo("420.00");
        assertThat(view.payable()).isEqualByComparingTo("50.00");
        assertThat(view.isSettledUp()).isFalse();
        assertThat(view.balances()).extracting(HomePageFacade.BalanceRow::name)
            .containsExactly("Tanvir", "Rahim", "Nabila");
        assertThat(view.balances().get(0).isOwedToYou()).isTrue();
        assertThat(view.balances().get(2).isOwedToYou()).isFalse();
        assertThat(view.balances().get(1).guest()).isTrue();
        assertThat(view.balances().get(1).isLinkable()).isFalse();
        assertThat(view.balances().get(0).isLinkable()).isTrue();
        assertThat(view.balances().get(0).initial()).isEqualTo("T");
    }

    @Test
    @DisplayName("Balances are ordered by size, largest first, whichever way the money flows")
    void ordersBySize() {
        givenLedger(List.of(balance(2, CounterpartyType.USER, "10.00"), balance(3, CounterpartyType.USER, "-500.00"),
            balance(4, CounterpartyType.USER, "200.00")));
        givenNoOutings();
        when(users.summariesOf(any())).thenReturn(Map.of());

        HomeView view = facade.forUser(1L, "Rafsan");

        assertThat(view.balances()).extracting(HomePageFacade.BalanceRow::counterpartyId)
            .containsExactly(3L, 4L, 2L);
    }

    @Test
    @DisplayName("Only the five biggest balances are shown and the rest are counted")
    void limitsBalances() {
        List<LedgerBalance> many = new ArrayList<>();
        for (int i = 1; i <= 8; i++) {
            many.add(balance(100 + i, CounterpartyType.USER, i + ".00"));
        }
        givenLedger(many);
        givenNoOutings();
        when(users.summariesOf(any())).thenReturn(Map.of());

        HomeView view = facade.forUser(1L, "Rafsan");

        assertThat(view.balances()).hasSize(HomePageFacade.BALANCE_LIMIT);
        assertThat(view.hiddenBalances()).isEqualTo(3);
        assertThat(view.balances().get(0).amount()).isEqualByComparingTo("8.00");
        assertThat(view.isSettledUp()).isFalse();
    }

    @Test
    @DisplayName("Unknown people fall back to a numbered name instead of failing")
    void unknownNamesFallBack() {
        givenLedger(List.of(balance(77, CounterpartyType.USER, "5.00"), balance(88, CounterpartyType.GUEST, "6.00")));
        givenNoOutings();
        when(users.summariesOf(any())).thenReturn(Map.of());
        when(guests.findAllById(any())).thenReturn(List.of());

        HomeView view = facade.forUser(1L, "Rafsan");

        assertThat(view.balances()).extracting(HomePageFacade.BalanceRow::name)
            .containsExactly("Guest #88", "User #77");
    }

    @Test
    @DisplayName("The dashboard shows the latest active outings only, capped, and counts drafts")
    void recentOutingsAndDrafts() {
        when(ledger.summaryFor(1L)).thenReturn(LedgerSummary.empty(1L));
        when(users.summariesOf(any())).thenReturn(Map.of());
        List<OutingRow> active = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            active.add(row(i, OutingStatus.ACTIVE));
        }
        when(outings.list(1L)).thenReturn(new OutingList(active,
            List.of(row(20, OutingStatus.DRAFT), row(21, OutingStatus.DRAFT)),
            List.of(row(30, OutingStatus.SETTLED))));

        HomeView view = facade.forUser(1L, "Rafsan");

        assertThat(view.recentOutings()).hasSize(HomePageFacade.OUTING_LIMIT);
        assertThat(view.recentOutings()).extracting(OutingRow::id).containsExactly(1L, 2L, 3L, 4L);
        assertThat(view.draftCount()).isEqualTo(2);
    }
}
