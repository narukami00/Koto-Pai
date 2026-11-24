package com.kotopai.ledger;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.kotopai.ledger.domain.Debt;
import com.kotopai.ledger.domain.GuestDebt;
import com.kotopai.ledger.domain.Payment;
import com.kotopai.ledger.repository.DebtRepository;
import com.kotopai.ledger.repository.GuestDebtRepository;
import com.kotopai.ledger.repository.PaymentRepository;

@SpringBootTest
@Transactional
class LedgerAuthorizationIT {

	private static final long DEBTOR = 82001L;
	private static final long CREDITOR = 82002L;
	private static final long MIDDLEMAN = 82003L;
	private static final long UNRELATED = 82999L;

	@Autowired WebApplicationContext context;
	@Autowired DebtRepository debts;
	@Autowired PaymentRepository payments;
	@Autowired GuestDebtRepository guestDebts;

	private MockMvc mvc;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
	}

	@ParameterizedTest
	@MethodSource("apiRequests")
	void everyLedgerAndActivityApiRequiresAuthentication(MockHttpServletRequestBuilder request) throws Exception {
		mvc.perform(request).andExpect(status().isUnauthorized());
	}

	@ParameterizedTest
	@MethodSource("pageRequests")
	void everyLedgerAndActivityPageRequiresAuthentication(MockHttpServletRequestBuilder request) throws Exception {
		mvc.perform(request).andExpect(status().is3xxRedirection());
	}

	@Test
	void unrelatedUserCannotReadOrMutateAnotherUsersLedgerResources() throws Exception {
		Debt debt = debts.saveAndFlush(Debt.forOuting(82010L, DEBTOR, CREDITOR, money("100.00")));
		Payment pending = payments.saveAndFlush(Payment.pending(debt, money("20.00"), DEBTOR));
		GuestDebt guestDebt = guestDebts.saveAndFlush(new GuestDebt(82010L, 82004L, CREDITOR, money("15.00")));
		Debt incoming = debts.saveAndFlush(Debt.forOuting(82011L, DEBTOR, MIDDLEMAN, money("30.00")));
		Debt outgoing = debts.saveAndFlush(Debt.forOuting(82012L, MIDDLEMAN, CREDITOR, money("30.00")));

		getAs(UNRELATED, "/api/ledger")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.net").value(0.00));
		getAs(UNRELATED, "/api/debts").andExpect(jsonPath("$").isEmpty());
		getAs(UNRELATED, "/api/guest-debts").andExpect(jsonPath("$").isEmpty());
		getAs(UNRELATED, "/api/delegations/suggestions").andExpect(jsonPath("$").isEmpty());
		getAs(UNRELATED, "/api/activity").andExpect(jsonPath("$").isEmpty());

		postAs(UNRELATED, "/api/debts/" + debt.getId() + "/payments", "{\"amount\":10.00}")
				.andExpect(status().isForbidden());
		postAs(UNRELATED, "/api/payments/" + pending.getId() + "/confirm", "")
				.andExpect(status().isForbidden());
		postAs(UNRELATED, "/api/debts/" + debt.getId() + "/dispute", "{\"reason\":\"Not mine\"}")
				.andExpect(status().isForbidden());
		postAs(UNRELATED, "/api/guest-debts/" + guestDebt.getId() + "/settle", "")
				.andExpect(status().isForbidden());
		postAs(UNRELATED, "/api/delegations", "{\"incomingDebtId\":" + incoming.getId()
				+ ",\"outgoingDebtId\":" + outgoing.getId() + ",\"amount\":10.00}")
				.andExpect(status().isForbidden());
	}

	private org.springframework.test.web.servlet.ResultActions getAs(long userId, String url) throws Exception {
		return mvc.perform(get(url).with(user(String.valueOf(userId))));
	}

	private org.springframework.test.web.servlet.ResultActions postAs(long userId, String url, String body)
			throws Exception {
		return mvc.perform(post(url).with(user(String.valueOf(userId))).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private static Stream<MockHttpServletRequestBuilder> apiRequests() {
		return Stream.of(
				get("/api/ledger"),
				get("/api/debts"),
				post("/api/debts/1/payments").with(csrf()).contentType(MediaType.APPLICATION_JSON)
						.content("{\"amount\":1.00}"),
				post("/api/payments/1/confirm").with(csrf()),
				post("/api/debts/1/dispute").with(csrf()).contentType(MediaType.APPLICATION_JSON)
						.content("{\"reason\":\"test\"}"),
				get("/api/guest-debts"),
				post("/api/guest-debts/1/settle").with(csrf()),
				get("/api/delegations/suggestions"),
				post("/api/delegations").with(csrf()).contentType(MediaType.APPLICATION_JSON)
						.content("{\"incomingDebtId\":1,\"outgoingDebtId\":2,\"amount\":1.00}"),
				get("/api/activity"));
	}

	private static Stream<MockHttpServletRequestBuilder> pageRequests() {
		return Stream.of(
				get("/ledger"),
				get("/ledger/friends/1"),
				get("/settle"),
				post("/settle/delegations").with(csrf())
						.param("incomingDebtId", "1").param("outgoingDebtId", "2").param("amount", "1.00"),
				get("/activity"));
	}

	private static BigDecimal money(String amount) {
		return new BigDecimal(amount);
	}
}
