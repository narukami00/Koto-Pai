package com.kotopai.home;

import com.kotopai.ledger.service.CounterpartyType;
import com.kotopai.ledger.service.LedgerBalance;
import com.kotopai.ledger.service.LedgerService;
import com.kotopai.ledger.service.LedgerSummary;
import com.kotopai.outing.controller.OutingPageFacade;
import com.kotopai.outing.controller.OutingPageFacade.OutingList;
import com.kotopai.outing.controller.OutingPageFacade.OutingRow;
import com.kotopai.outing.domain.Guest;
import com.kotopai.outing.repository.GuestRepository;
import com.kotopai.user.dto.UserSummary;
import com.kotopai.user.service.UserService;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Facade for the home dashboard: one call gathers the signed-in user's money position (from the ledger), the people
 * behind each balance (users and guests, by name) and their latest outings, so the template gets plain records.
 */
@Service
@Transactional(readOnly = true)
public class HomePageFacade {

    static final int BALANCE_LIMIT = 5;
    static final int OUTING_LIMIT = 4;

    /** One counterparty balance. Guests have no friend page, so they are not linkable. */
    public record BalanceRow(long counterpartyId, String name, String initial, boolean guest, BigDecimal amount) {
        public boolean isOwedToYou() {
            return amount.signum() > 0;
        }

        public boolean isLinkable() {
            return !guest;
        }
    }

    public record HomeView(String displayName, BigDecimal net, BigDecimal receivable, BigDecimal payable,
                           List<BalanceRow> balances, int hiddenBalances, List<OutingRow> recentOutings,
                           int draftCount) {
        public boolean isSettledUp() {
            return balances.isEmpty() && hiddenBalances == 0;
        }
    }

    private final LedgerService ledger;
    private final OutingPageFacade outings;
    private final UserService users;
    private final GuestRepository guests;

    public HomePageFacade(LedgerService ledger, OutingPageFacade outings, UserService users, GuestRepository guests) {
        this.ledger = Objects.requireNonNull(ledger);
        this.outings = Objects.requireNonNull(outings);
        this.users = Objects.requireNonNull(users);
        this.guests = Objects.requireNonNull(guests);
    }

    public HomeView forUser(long userId, String displayName) {
        LedgerSummary summary = ledger.summaryFor(userId);
        List<LedgerBalance> biggest = summary.balances().stream()
            .sorted((a, b) -> b.balance().abs().compareTo(a.balance().abs()))
            .toList();
        List<LedgerBalance> shown = biggest.stream().limit(BALANCE_LIMIT).toList();

        OutingList list = outings.list(userId);
        List<OutingRow> recent = list.active().stream().limit(OUTING_LIMIT).toList();
        return new HomeView(displayName, summary.net(), summary.totalReceivable(), summary.totalPayable(),
            balanceRows(shown), biggest.size() - shown.size(), recent, list.drafts().size());
    }

    private List<BalanceRow> balanceRows(List<LedgerBalance> balances) {
        Set<Long> userIds = balances.stream().filter(b -> b.type() == CounterpartyType.USER)
            .map(LedgerBalance::counterpartyId).collect(Collectors.toSet());
        Set<Long> guestIds = balances.stream().filter(b -> b.type() == CounterpartyType.GUEST)
            .map(LedgerBalance::counterpartyId).collect(Collectors.toSet());
        Map<Long, UserSummary> userNames = users.summariesOf(userIds);
        Map<Long, String> guestNames = new HashMap<>();
        for (Guest g : guests.findAllById(guestIds)) {
            guestNames.put(g.getId(), g.getName());
        }
        return balances.stream().map(b -> {
            boolean guest = b.type() == CounterpartyType.GUEST;
            String name = guest
                ? guestNames.getOrDefault(b.counterpartyId(), "Guest #" + b.counterpartyId())
                : (userNames.containsKey(b.counterpartyId())
                    ? userNames.get(b.counterpartyId()).displayName() : "User #" + b.counterpartyId());
            String initial = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase();
            return new BalanceRow(b.counterpartyId(), name, initial, guest, b.balance());
        }).toList();
    }
}
