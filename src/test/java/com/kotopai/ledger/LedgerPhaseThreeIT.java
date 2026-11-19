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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.jayway.jsonpath.JsonPath;
import com.kotopai.friend.domain.Friendship;
import com.kotopai.friend.repository.FriendshipRepository;
import com.kotopai.ledger.domain.Debt;
import com.kotopai.ledger.domain.DebtStatus;
import com.kotopai.ledger.domain.DelegationStatus;
import com.kotopai.ledger.repository.DebtRepository;
import com.kotopai.ledger.repository.DelegationRepository;
import com.kotopai.ledger.repository.PaymentRepository;
import com.kotopai.user.domain.Role;
import com.kotopai.user.domain.User;
import com.kotopai.user.repository.UserRepository;

@SpringBootTest
class LedgerPhaseThreeIT {

	@Autowired WebApplicationContext context;
	@Autowired DebtRepository debts;
	@Autowired PaymentRepository payments;
	@Autowired DelegationRepository delegations;
	@Autowired FriendshipRepository friendships;
	@Autowired UserRepository users;

	private MockMvc mvc;
	private long debtorId;
	private long middlemanId;
	private long creditorId;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
		delegations.deleteAll();
		payments.deleteAll();
		debts.deleteAll();
		friendships.deleteAll();
		users.deleteAll();

		User debtor = users.save(new User("nabila", "Nabila", "nabila@example.com", "test-hash", Role.USER));
		User middleman = users.save(new User("samiha", "Samiha", "samiha@example.com", "test-hash", Role.USER));
		User creditor = users.save(new User("rafsan", "Rafsan", "rafsan@example.com", "test-hash", Role.USER));
		debtorId = debtor.getId();
		middlemanId = middleman.getId();
		creditorId = creditor.getId();
		Friendship outerUsers = new Friendship(debtor, creditor);
		outerUsers.accept();
		friendships.saveAndFlush(outerUsers);
	}

	@AfterEach
	void tearDown() {
		delegations.deleteAll();
		payments.deleteAll();
		debts.deleteAll();
		friendships.deleteAll();
		users.deleteAll();
	}

	@Test
	void delegatesImmediatelyAndDisputeReversesEveryDebtAndTimelineEntry() throws Exception {
		Debt incoming = debts.saveAndFlush(Debt.forOuting(90011L, debtorId, middlemanId, money("100.00")));
		Debt outgoing = debts.saveAndFlush(Debt.forOuting(90012L, middlemanId, creditorId, money("50.00")));

		String body = "{\"incomingDebtId\":" + incoming.getId()
				+ ",\"outgoingDebtId\":" + outgoing.getId() + ",\"amount\":30.00}";
		MvcResult result = postAs(middlemanId, "/api/delegations", body)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.debtorId").value(debtorId))
				.andExpect(jsonPath("$.middlemanId").value(middlemanId))
				.andExpect(jsonPath("$.creditorId").value(creditorId))
				.andExpect(jsonPath("$.status").value("EXECUTED"))
				.andReturn();
		long delegatedDebtId = ((Number) JsonPath.read(
				result.getResponse().getContentAsString(), "$.delegatedDebtId")).longValue();

		assertThat(debts.findById(incoming.getId()).orElseThrow().getRemaining()).isEqualByComparingTo("70.00");
		assertThat(debts.findById(outgoing.getId()).orElseThrow().getRemaining()).isEqualByComparingTo("20.00");
		assertThat(debts.findById(delegatedDebtId).orElseThrow().getRemaining()).isEqualByComparingTo("30.00");
		getAs(middlemanId, "/api/ledger")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalReceivable").value(70.00))
				.andExpect(jsonPath("$.totalPayable").value(20.00))
				.andExpect(jsonPath("$.net").value(50.00));
		getAs(debtorId, "/api/ledger")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalPayable").value(100.00));

		postAs(debtorId, "/api/debts/" + delegatedDebtId + "/dispute",
				"{\"reason\":\"Please keep the original route\"}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("VOID"));

		assertThat(debts.findById(incoming.getId()).orElseThrow().getRemaining()).isEqualByComparingTo("100.00");
		assertThat(debts.findById(outgoing.getId()).orElseThrow().getRemaining()).isEqualByComparingTo("50.00");
		assertThat(debts.findById(delegatedDebtId).orElseThrow().getStatus()).isEqualTo(DebtStatus.VOID);
		assertThat(delegations.findByDelegatedDebtId(delegatedDebtId)).isPresent()
				.get().extracting(delegation -> delegation.getStatus()).isEqualTo(DelegationStatus.REVERSED);
	}

	@Test
	void onlyMiddlemanCanExecuteAndAuthenticationIsRequired() throws Exception {
		Debt incoming = debts.saveAndFlush(Debt.forOuting(90011L, debtorId, middlemanId, money("40.00")));
		Debt outgoing = debts.saveAndFlush(Debt.forOuting(90012L, middlemanId, creditorId, money("40.00")));
		String body = "{\"incomingDebtId\":" + incoming.getId()
				+ ",\"outgoingDebtId\":" + outgoing.getId() + ",\"amount\":20.00}";

		postAs(debtorId, "/api/delegations", body).andExpect(status().isForbidden());
		mvc.perform(post("/api/delegations").with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isUnauthorized());
	}

	private org.springframework.test.web.servlet.ResultActions postAs(long userId, String url, String body)
			throws Exception {
		return mvc.perform(post(url).with(user(String.valueOf(userId))).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private org.springframework.test.web.servlet.ResultActions getAs(long userId, String url) throws Exception {
		return mvc.perform(get(url).with(user(String.valueOf(userId))));
	}

	private static BigDecimal money(String amount) {
		return new BigDecimal(amount);
	}
}
