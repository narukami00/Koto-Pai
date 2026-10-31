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
	private static final RoundingMode MONEY_ROUNDING = RoundingMode.HALF_UP;
	private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(MONEY_SCALE);

	/**
	 * Simplifies balanced net positions into debtor-to-creditor transfers.
	 * Amounts are rounded to two decimal places before they are balanced.
	 *
	 * @param positions participant net positions; positive receives, negative owes
	 * @return an immutable list of transfers, each with a positive two-decimal amount
	 * @throws IllegalArgumentException if participants are invalid or rounded nets do not balance
	 */
	public List<Transfer> simplify(List<NetPosition> positions) {
		Objects.requireNonNull(positions, "positions must not be null");

		PriorityQueue<Balance> creditors = new PriorityQueue<>();
		PriorityQueue<Balance> debtors = new PriorityQueue<>();
		Set<String> participants = new HashSet<>();
		BigDecimal total = ZERO;
		int inputOrder = 0;

		for (NetPosition position : positions) {
			if (position == null) {
				throw new IllegalArgumentException("positions must not contain null");
			}

			String participant = validateParticipant(position.participant());
			if (!participants.add(participant)) {
				throw new IllegalArgumentException("duplicate participant: " + participant);
			}

			BigDecimal amount = normalize(position.amount());
			total = total.add(amount);
			if (amount.signum() > 0) {
				creditors.add(new Balance(participant, amount, inputOrder));
			} else if (amount.signum() < 0) {
				debtors.add(new Balance(participant, amount.abs(), inputOrder));
			}
			inputOrder++;
		}

		if (total.compareTo(ZERO) != 0) {
			throw new IllegalArgumentException(
					"rounded net positions must sum to 0.00, but summed to " + total);
		}

		List<Transfer> transfers = new ArrayList<>();

		while (!debtors.isEmpty() && !creditors.isEmpty()) {
			Balance debtor = debtors.remove();
			Balance creditor = creditors.remove();
			BigDecimal amount = debtor.remaining().min(creditor.remaining());

			transfers.add(new Transfer(debtor.participant(), creditor.participant(), amount));
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

	private static String validateParticipant(String participant) {
		if (participant == null || participant.isBlank()) {
			throw new IllegalArgumentException("participant must not be blank");
		}
		return participant;
	}

	private static BigDecimal normalize(BigDecimal amount) {
		if (amount == null) {
			throw new IllegalArgumentException("amount must not be null");
		}
		return amount.setScale(MONEY_SCALE, MONEY_ROUNDING);
	}

	private record Balance(String participant, BigDecimal remaining, int inputOrder)
			implements Comparable<Balance> {

		private Balance subtract(BigDecimal amount) {
			return new Balance(participant, remaining.subtract(amount), inputOrder);
		}

		@Override
		public int compareTo(Balance other) {
			int amountOrder = other.remaining.compareTo(remaining);
			return amountOrder != 0 ? amountOrder : Integer.compare(inputOrder, other.inputOrder);
		}
	}
}
