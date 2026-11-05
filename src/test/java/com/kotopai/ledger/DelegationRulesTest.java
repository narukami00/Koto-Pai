package com.kotopai.ledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class DelegationRulesTest {

	@Test
	void calculatesPartialDelegationChanges() {
		DelegationRules rules = new DelegationRules((userA, userB) -> true);

		DelegationPlan plan = rules.plan(
				debt(1, 10, 20, "400.00"),
				debt(2, 20, 30, "350.00"),
				20,
				new BigDecimal("300.00"));

		assertThat(plan.middlemanId()).isEqualTo(20);
		assertThat(plan.incomingDebt()).isEqualTo(
				new DebtAdjustment(1, money("400.00"), money("100.00")));
		assertThat(plan.outgoingDebt()).isEqualTo(
				new DebtAdjustment(2, money("350.00"), money("50.00")));
		assertThat(plan.delegatedDebt()).isEqualTo(
				new ProposedDebt(10, 30, money("300.00")));
	}

	@Test
	void rejectsDelegationBetweenNonFriends() {
		DelegationRules rules = new DelegationRules((userA, userB) -> false);

		assertThatThrownBy(() -> rules.plan(
				debt(1, 10, 20, "400.00"),
				debt(2, 20, 30, "350.00"),
				20,
				money("100.00")))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("friends");
	}

	@Test
	void rejectsAmountAboveEitherRemainingBalance() {
		DelegationRules rules = new DelegationRules((userA, userB) -> true);

		assertThatThrownBy(() -> rules.plan(
				debt(1, 10, 20, "400.00"),
				debt(2, 20, 30, "350.00"),
				20,
				money("350.01")))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("exceeds");
	}

	private static OpenDebt debt(long id, long debtor, long creditor, String remaining) {
		return new OpenDebt(id, debtor, creditor, money(remaining));
	}

	private static BigDecimal money(String amount) {
		return new BigDecimal(amount);
	}
}
