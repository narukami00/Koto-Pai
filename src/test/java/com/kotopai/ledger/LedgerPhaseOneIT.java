package com.kotopai.ledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.kotopai.ledger.domain.Debt;
import com.kotopai.ledger.domain.DebtStatus;
import com.kotopai.ledger.domain.GuestDebt;
import com.kotopai.ledger.domain.Payment;
import com.kotopai.ledger.repository.DebtRepository;
import com.kotopai.ledger.repository.GuestDebtRepository;
import com.kotopai.ledger.repository.PaymentRepository;
import com.kotopai.ledger.service.CounterpartyType;
import com.kotopai.ledger.service.DebtService;
import com.kotopai.ledger.service.LedgerBalance;
import com.kotopai.ledger.service.LedgerService;
import com.kotopai.ledger.service.LedgerSummary;
import com.kotopai.ledger.spi.GuestNetPosition;
import com.kotopai.ledger.spi.OutingSettlementProvider;
import com.kotopai.ledger.spi.OutingSettlementSnapshot;
import com.kotopai.ledger.spi.UserNetPosition;

@SpringBootTest
@Transactional
class LedgerPhaseOneIT {

	@Autowired
	private DebtService debtService;

	@Autowired
	private LedgerService ledgerService;

	@Autowired
	private DebtRepository debts;

	@Autowired
	private PaymentRepository payments;

	@Autowired
	private GuestDebtRepository guestDebts;

	@Autowired
	private StubSettlementProvider settlements;

	@Autowired
	private WebApplicationContext webContext;

	private MockMvc mvc;

	@BeforeEach
	void cleanDatabase() {
		mvc = MockMvcBuilders.webAppContextSetup(webContext).apply(springSecurity()).build();
		payments.deleteAll();
		guestDebts.deleteAll();
		debts.deleteAll();
		settlements.clear();
	}

	@Test
	void generatesSimplifiedDebtsAndGuestProxyRecords() {
		settlements.put(new OutingSettlementSnapshot(
				100L,
				List.of(
						new UserNetPosition(1L, money("500.00")),
						new UserNetPosition(2L, money("-300.00"))),
				List.of(new GuestNetPosition(9L, 1L, money("-200.00")))));

		debtService.generateDebts(100L);

		assertThat(debts.findAllByOutingId(100L))
				.singleElement()
				.satisfies(debt -> {
					assertThat(debt.getDebtorId()).isEqualTo(2L);
					assertThat(debt.getCreditorId()).isEqualTo(1L);
					assertThat(debt.getAmount()).isEqualByComparingTo("300.00");
					assertThat(debt.getStatus()).isEqualTo(DebtStatus.OPEN);
				});
		assertThat(guestDebts.findAllByOutingId(100L))
				.singleElement()
				.satisfies(guestDebt -> {
					assertThat(guestDebt.getGuestId()).isEqualTo(9L);
					assertThat(guestDebt.getOwnerId()).isEqualTo(1L);
					assertThat(guestDebt.getAmount()).isEqualByComparingTo("200.00");
				});
	}

	@Test
	void detectsPaymentsAndVoidsOutingDebts() {
		Debt debt = debts.save(Debt.forOuting(200L, 2L, 1L, money("75.00")));
		assertThat(debtService.hasRecordedPayments(200L)).isFalse();

		payments.save(Payment.pending(debt, money("25.00"), 2L));
		assertThat(debtService.hasRecordedPayments(200L)).isTrue();

		debtService.voidDebts(200L);
		assertThat(debt.getStatus()).isEqualTo(DebtStatus.VOID);
	}

	@Test
	void calculatesBalancesAndExcludesDisputedDebts() {
		debts.save(Debt.forOuting(1L, 2L, 1L, money("100.00")));
		debts.save(Debt.forOuting(2L, 1L, 3L, money("40.00")));
		Debt disputed = Debt.forOuting(3L, 4L, 1L, money("60.00"));
		disputed.dispute();
		debts.save(disputed);
		guestDebts.save(new GuestDebt(1L, 9L, 1L, money("25.00")));

		LedgerSummary summary = ledgerService.summaryFor(1L);

		assertThat(summary.totalReceivable()).isEqualByComparingTo("125.00");
		assertThat(summary.totalPayable()).isEqualByComparingTo("40.00");
		assertThat(summary.net()).isEqualByComparingTo("85.00");
		assertThat(summary.balances()).containsExactly(
				new LedgerBalance(2L, CounterpartyType.USER, money("100.00")),
				new LedgerBalance(3L, CounterpartyType.USER, money("-40.00")),
				new LedgerBalance(9L, CounterpartyType.GUEST, money("25.00")));
		assertThat(summary.balances())
				.noneMatch(balance -> balance.counterpartyId() == 4L);
	}

	@Test
	void returnsCompletePerFriendHistory() {
		debts.save(Debt.forOuting(1L, 2L, 1L, money("100.00")));
		Debt disputed = Debt.forOuting(2L, 1L, 2L, money("25.00"));
		disputed.dispute();
		debts.save(disputed);
		debts.save(Debt.forOuting(3L, 3L, 1L, money("50.00")));

		assertThat(ledgerService.historyBetween(1L, 2L))
				.hasSize(2)
				.extracting(item -> item.status())
				.containsExactlyInAnyOrder(DebtStatus.OPEN, DebtStatus.DISPUTED);
	}

	@Test
	void rendersLedgerAndFriendHistoryTemplates() throws Exception {
		debts.save(Debt.forOuting(1L, 2L, 1L, money("100.00")));

		mvc.perform(get("/ledger").param("userId", "1").with(user("test-user")))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Net balance")))
				.andExpect(content().string(containsString("Friend #2")))
				.andExpect(content().string(containsString("৳100.00")));

		mvc.perform(get("/ledger/friends/2").param("userId", "1").with(user("test-user")))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("History with Friend #2")))
				.andExpect(content().string(containsString("Friend #2 → You")));
	}

	private static BigDecimal money(String amount) {
		return new BigDecimal(amount);
	}

	@TestConfiguration
	static class SettlementTestConfiguration {

		@Bean
		@Primary
		StubSettlementProvider stubSettlementProvider() {
			return new StubSettlementProvider();
		}
	}

	static final class StubSettlementProvider implements OutingSettlementProvider {

		private final Map<Long, OutingSettlementSnapshot> snapshots = new ConcurrentHashMap<>();

		void put(OutingSettlementSnapshot snapshot) {
			snapshots.put(snapshot.outingId(), snapshot);
		}

		void clear() {
			snapshots.clear();
		}

		@Override
		public OutingSettlementSnapshot settlementFor(long outingId) {
			OutingSettlementSnapshot snapshot = snapshots.get(outingId);
			if (snapshot == null) {
				throw new IllegalArgumentException("no test snapshot for outing " + outingId);
			}
			return snapshot;
		}
	}
}
