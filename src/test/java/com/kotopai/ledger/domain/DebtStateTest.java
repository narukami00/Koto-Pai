package com.kotopai.ledger.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class DebtStateTest {

	@Test
	void movesFromOpenThroughPartialToPaid() {
		Debt debt = Debt.forOuting(50L, 10L, 20L, money("100.00"));

		debt.applyConfirmedPayment(money("30.00"));
		assertThat(debt.getStatus()).isEqualTo(DebtStatus.PARTIALLY_PAID);
		assertThat(debt.getRemaining()).isEqualByComparingTo("70.00");

		debt.applyConfirmedPayment(money("20.00"));
		assertThat(debt.getStatus()).isEqualTo(DebtStatus.PARTIALLY_PAID);
		assertThat(debt.getRemaining()).isEqualByComparingTo("50.00");

		debt.applyConfirmedPayment(money("50.00"));
		assertThat(debt.getStatus()).isEqualTo(DebtStatus.PAID);
		assertThat(debt.getRemaining()).isEqualByComparingTo("0.00");
	}

	@Test
	void disputedDebtCanOnlyReopenOrBecomeVoid() {
		Debt debt = Debt.forOuting(50L, 10L, 20L, money("100.00"));
		debt.dispute();

		assertThat(debt.getStatus()).isEqualTo(DebtStatus.DISPUTED);
		assertThat(debt.getStatus().contributesToBalance()).isFalse();

		debt.reopen();
		assertThat(debt.getStatus()).isEqualTo(DebtStatus.OPEN);
	}

	@Test
	void rejectsOverpayment() {
		Debt debt = Debt.forOuting(50L, 10L, 20L, money("100.00"));

		assertThatThrownBy(() -> debt.applyConfirmedPayment(money("100.01")))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("exceed");
	}

	private static BigDecimal money(String amount) {
		return new BigDecimal(amount);
	}
}
