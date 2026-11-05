package com.kotopai.ledger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Converts balanced net positions into a small set of direct transfers.
 *
 * <p>The largest remaining debtor is matched with the largest remaining
 * creditor on every iteration. Each transfer settles at least one participant,
 * so {@code n} non-zero positions produce at most {@code n - 1} transfers.</p>
 */
public final class DebtSimplifier {

	private static final int MONEY_SCALE = 2;
	private static final RoundingMode MONEY_ROUNDING = RoundingMode.HALF_EVEN;
	private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(MONEY_SCALE);

	/**
	 * Simplifies balanced net positions into debtor-to-creditor transfers.
	 * Amounts are rounded to two decimal places before they are balanced.
	 *
	 * @param positions participant net positions; positive receives, negative owes
	 * @return an immutable list of transfers, each with a positive two-decimal amount
	 * @throws IllegalArgumentException if participants are invalid or rounded nets do not balance
	 */
	public <T> List<Transfer<T>> simplify(List<NetPosition<T>> positions) {
		Objects.requireNonNull(positions, "positions must not be null");

		PriorityQueue<Balance<T>> creditors = new PriorityQueue<>(largestFirst());
		PriorityQueue<Balance<T>> debtors = new PriorityQueue<>(largestFirst());
		Set<T> participants = new HashSet<>();
		BigDecimal total = ZERO;
		int inputOrder = 0;

		for (NetPosition<T> position : positions) {
			if (position == null) {
				throw new IllegalArgumentException("positions must not contain null");
			}

			T participant = Objects.requireNonNull(position.participant(), "participant must not be null");
			if (!participants.add(participant)) {
				throw new IllegalArgumentException("duplicate participant: " + participant);
			}

			BigDecimal amount = normalize(position.amount());
			total = total.add(amount);
			if (amount.signum() > 0) {
				creditors.add(new Balance<>(participant, amount, inputOrder));
			} else if (amount.signum() < 0) {
				debtors.add(new Balance<>(participant, amount.abs(), inputOrder));
			}
			inputOrder++;
		}

		if (total.compareTo(ZERO) != 0) {
			throw new IllegalArgumentException(
					"rounded net positions must sum to 0.00, but summed to " + total);
		}

		List<Transfer<T>> transfers = new ArrayList<>();

		while (!debtors.isEmpty() && !creditors.isEmpty()) {
			Balance<T> debtor = debtors.remove();
			Balance<T> creditor = creditors.remove();
			BigDecimal amount = debtor.remaining().min(creditor.remaining());

			transfers.add(new Transfer<>(debtor.participant(), creditor.participant(), amount));
			debtor = debtor.subtract(amount);
			creditor = creditor.subtract(amount);

			if (debtor.remaining().compareTo(ZERO) > 0) {
				debtors.add(debtor);
			}
			if (creditor.remaining().compareTo(ZERO) > 0) {
				creditors.add(creditor);
			}
		}

		return List.copyOf(transfers);
	}

	private static BigDecimal normalize(BigDecimal amount) {
		if (amount == null) {
			throw new IllegalArgumentException("amount must not be null");
		}
		return amount.setScale(MONEY_SCALE, MONEY_ROUNDING);
	}

	private static <T> java.util.Comparator<Balance<T>> largestFirst() {
		return (left, right) -> {
			int amountOrder = right.remaining.compareTo(left.remaining);
			return amountOrder != 0 ? amountOrder : Integer.compare(left.inputOrder, right.inputOrder);
		};
	}

	private record Balance<T>(T participant, BigDecimal remaining, int inputOrder) {

		private Balance<T> subtract(BigDecimal amount) {
			return new Balance<>(participant, remaining.subtract(amount), inputOrder);
		}
	}
}
