package com.kotopai.ledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.http.MediaType;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.jayway.jsonpath.JsonPath;
import com.kotopai.ledger.domain.Debt;
import com.kotopai.ledger.domain.DebtStatus;
import com.kotopai.ledger.domain.GuestDebt;
import com.kotopai.ledger.domain.GuestDebtStatus;
import com.kotopai.ledger.event.DebtDisputedEvent;
import com.kotopai.ledger.event.PaymentConfirmedEvent;
import com.kotopai.ledger.event.PaymentRecordedEvent;
import com.kotopai.ledger.repository.DebtRepository;
import com.kotopai.ledger.repository.GuestDebtRepository;
import com.kotopai.ledger.repository.PaymentRepository;
import com.kotopai.outing.domain.Outing;
import com.kotopai.outing.domain.OutingCategory;
import com.kotopai.outing.domain.OutingStatus;
import com.kotopai.outing.repository.OutingRepository;

@SpringBootTest
@Transactional
@RecordApplicationEvents
class LedgerPhaseTwoIT {

	@Autowired WebApplicationContext context;
	@Autowired DebtRepository debts;
	@Autowired PaymentRepository payments;
	@Autowired GuestDebtRepository guestDebts;
	@Autowired OutingRepository outings;
	@Autowired ApplicationEvents events;

	private MockMvc mvc;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
		payments.deleteAll();
		debts.deleteAll();
	}

	@Test
	void debtorRecordsCreditorConfirmsAndFinalPaymentSettlesOuting() throws Exception {
		Outing outing = activeOuting(9L);
		Debt debt = debts.saveAndFlush(Debt.forOuting(outing.getId(), 2L, 1L, money("100.00")));

		MvcResult pendingResult = postAs(2L, "/api/debts/" + debt.getId() + "/payments", "{\"amount\":40.00}")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("PENDING_CONFIRMATION"))
				.andReturn();
		long paymentId = ((Number) JsonPath.read(pendingResult.getResponse().getContentAsString(), "$.id")).longValue();
		assertThat(debts.findById(debt.getId()).orElseThrow().getRemaining()).isEqualByComparingTo("100.00");

		postAs(2L, "/api/payments/" + paymentId + "/confirm", "")
				.andExpect(status().isForbidden());
		postAs(1L, "/api/payments/" + paymentId + "/confirm", "")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CONFIRMED"));
		Debt partial = debts.findById(debt.getId()).orElseThrow();
		assertThat(partial.getRemaining()).isEqualByComparingTo("60.00");
		assertThat(partial.getStatus()).isEqualTo(DebtStatus.PARTIALLY_PAID);
		assertThat(outings.findById(outing.getId()).orElseThrow().getStatus()).isEqualTo(OutingStatus.ACTIVE);

		postAs(1L, "/api/debts/" + debt.getId() + "/payments", "{\"amount\":60.00}")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("CONFIRMED"));
		Debt paid = debts.findById(debt.getId()).orElseThrow();
		assertThat(paid.getRemaining()).isEqualByComparingTo("0.00");
		assertThat(paid.getStatus()).isEqualTo(DebtStatus.PAID);
		assertThat(outings.findById(outing.getId()).orElseThrow().getStatus()).isEqualTo(OutingStatus.SETTLED);
		assertThat(payments.findAllByDebtIdOrderByCreatedAtDesc(debt.getId())).hasSize(2);
		assertThat(countOf(PaymentRecordedEvent.class)).isEqualTo(2);
		assertThat(countOf(PaymentConfirmedEvent.class)).isEqualTo(2);
	}

	@Test
	void disputeStoresReasonPublishesCreatorEventAndRemovesBalance() throws Exception {
		Outing outing = activeOuting(9L);
		Debt debt = debts.saveAndFlush(Debt.forOuting(outing.getId(), 2L, 1L, money("75.00")));

		postAs(2L, "/api/debts/" + debt.getId() + "/dispute", "{\"reason\":\"Wrong participant share\"}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("DISPUTED"))
				.andExpect(jsonPath("$.disputeReason").value("Wrong participant share"));

		getAs(1L, "/api/ledger")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalReceivable").value(0.00));
		Debt disputed = debts.findById(debt.getId()).orElseThrow();
		assertThat(disputed.getStatus()).isEqualTo(DebtStatus.DISPUTED);
		DebtDisputedEvent event = payload(DebtDisputedEvent.class);
		assertThat(event.outingCreatorId()).isEqualTo(9L);
		assertThat(event.disputedById()).isEqualTo(2L);
	}

	@Test
	void onlyOwnerCanSettleGuestDebtAndEntryRemainsInHistory() throws Exception {
		Outing outing = activeOuting(1L);
		GuestDebt guestDebt = guestDebts.saveAndFlush(new GuestDebt(outing.getId(), 8L, 1L, money("30.00")));

		postAs(2L, "/api/guest-debts/" + guestDebt.getId() + "/settle", "")
				.andExpect(status().isForbidden());
		postAs(1L, "/api/guest-debts/" + guestDebt.getId() + "/settle", "")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("SETTLED"));
		getAs(1L, "/api/guest-debts")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].status").value("SETTLED"));

		assertThat(guestDebts.findById(guestDebt.getId())).isPresent()
				.get().extracting(GuestDebt::getStatus).isEqualTo(GuestDebtStatus.SETTLED);
	}

	@Test
	void ledgerSettlementEndpointsRequireAuthenticationAndValidAmounts() throws Exception {
		Outing outing = activeOuting(1L);
		Debt debt = debts.saveAndFlush(Debt.forOuting(outing.getId(), 2L, 1L, money("10.00")));

		mvc.perform(post("/api/debts/" + debt.getId() + "/payments").with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("{\"amount\":5.00}"))
				.andExpect(status().isUnauthorized());
		postAs(2L, "/api/debts/" + debt.getId() + "/payments", "{\"amount\":10.001}")
				.andExpect(status().isBadRequest());
		postAs(2L, "/api/debts/" + debt.getId() + "/payments", "{\"amount\":11.00}")
				.andExpect(status().isConflict());
	}

	private Outing activeOuting(long creatorId) {
		Outing outing = new Outing("Burger night", OutingCategory.EATING, LocalDate.of(2026, 11, 15), creatorId);
		outing.addUserParticipant(creatorId);
		outing.submit();
		return outings.saveAndFlush(outing);
	}

	private org.springframework.test.web.servlet.ResultActions postAs(long userId, String url, String body) throws Exception {
		return mvc.perform(post(url).with(user(String.valueOf(userId))).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private org.springframework.test.web.servlet.ResultActions getAs(long userId, String url) throws Exception {
		return mvc.perform(get(url).with(user(String.valueOf(userId))));
	}

	private long countOf(Class<?> type) {
		return events.stream()
				.filter(event -> event instanceof PayloadApplicationEvent<?> payload && type.isInstance(payload.getPayload()))
				.count();
	}

	private <T> T payload(Class<T> type) {
		return events.stream()
				.filter(event -> event instanceof PayloadApplicationEvent<?> payload && type.isInstance(payload.getPayload()))
				.map(event -> type.cast(((PayloadApplicationEvent<?>) event).getPayload()))
				.findFirst()
				.orElseThrow();
	}

	private static BigDecimal money(String amount) {
		return new BigDecimal(amount);
	}
}
