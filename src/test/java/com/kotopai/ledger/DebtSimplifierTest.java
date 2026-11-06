package com.kotopai.ledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

class DebtSimplifierTest {

	private final DebtSimplifier simplifier = new DebtSimplifier();

	@Test
	void simplifiesBurgerNight() {
		List<Transfer> transfers = simplifier.simplify(List.of(
				position("Rafsan", "700"),
				position("Tanvir", "-300"),
				position("Nabila", "-400")));

		assertThat(transfers).containsExactly(
				transfer("Nabila", "Rafsan", "400.00"),
				transfer("Tanvir", "Rafsan", "300.00"));
	}

	@Test
	void returnsNoTransfersWhenAllPositionsAreZero() {
		List<Transfer> transfers = simplifier.simplify(List.of(
				position("Rafsan", "0"),
				position("Tanvir", "0.00"),
				position("Nabila", "0.000")));

		assertThat(transfers).isEmpty();
	}

	@Test
	void simplifiesSingleDebtorAndCreditor() {
		List<Transfer> transfers = simplifier.simplify(List.of(
				position("Debtor", "-250"),
				position("Creditor", "250")));

		assertThat(transfers).containsExactly(
				transfer("Debtor", "Creditor", "250.00"));
	}

	@Test
	void returnsNoTransfersForCycleThatNetsToZero() {
		// A -> B, B -> C and C -> A for the same amount leaves every net at zero.
		List<Transfer> transfers = simplifier.simplify(List.of(
				position("A", "0"),
				position("B", "0"),
				position("C", "0")));

		assertThat(transfers).isEmpty();
	}

	@Test
	void roundsTransfersToTwoDecimalPlaces() {
		List<Transfer> transfers = simplifier.simplify(List.of(
				position("Debtor", "-123.456"),
				position("Creditor", "123.456")));

		assertThat(transfers).containsExactly(
				transfer("Debtor", "Creditor", "123.46"));
		assertThat(transfers.get(0).amount().scale()).isEqualTo(2);
	}

	@Test
	void handlesManyDebtorsAndOneCreditorWithinTransferLimit() {
		List<NetPosition> positions = List.of(
				position("Creditor", "100.00"),
				position("A", "-10.00"),
				position("B", "-30.00"),
				position("C", "-60.00"));

		List<Transfer> transfers = simplifier.simplify(positions);

		assertThat(transfers).containsExactly(
				transfer("C", "Creditor", "60.00"),
				transfer("B", "Creditor", "30.00"),
				transfer("A", "Creditor", "10.00"));
		assertThat(transfers).hasSizeLessThanOrEqualTo(positions.size() - 1);
	}

	@Test
	void reprioritizesPartiallySettledBalances() {
		List<Transfer> transfers = simplifier.simplify(List.of(
				position("Debtor A", "-100.00"),
				position("Debtor B", "-90.00"),
				position("Creditor X", "50.00"),
				position("Creditor Y", "140.00")));

		assertThat(transfers).containsExactly(
				transfer("Debtor A", "Creditor Y", "100.00"),
				transfer("Debtor B", "Creditor X", "50.00"),
				transfer("Debtor B", "Creditor Y", "40.00"));
	}

	@Test
	void rejectsPositionsThatDoNotConserveMoneyAfterRounding() {
		assertThatThrownBy(() -> simplifier.simplify(List.of(
				position("A", "10.00"),
				position("B", "-9.99"))))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("must sum to 0.00");
	}

	private static NetPosition position(String participant, String amount) {
		return new NetPosition(participant, new BigDecimal(amount));
	}

	private static Transfer transfer(String debtor, String creditor, String amount) {
		return new Transfer(debtor, creditor, new BigDecimal(amount));
	}
}
