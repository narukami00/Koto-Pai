package com.kotopai.ledger;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class DelegationSuggesterTest {

	@Test
	void suggestsEveryMutualFriendChainAtTheSafeMaximum() {
		Set<String> friendships = Set.of("10:30", "11:30");
		DelegationSuggester suggester = new DelegationSuggester(
				(userA, userB) -> friendships.contains(userA + ":" + userB));

		List<DelegationSuggestion> suggestions = suggester.suggest(List.of(
				debt(1, 10, 20, "400.00"),
				debt(2, 11, 20, "100.00"),
				debt(3, 20, 30, "350.00")));

		assertThat(suggestions).containsExactly(
				new DelegationSuggestion(1, 3, 10, 20, 30, money("350.00")),
				new DelegationSuggestion(2, 3, 11, 20, 30, money("100.00")));
	}

	@Test
	void returnsNoSuggestionWhenOuterUsersAreNotFriends() {
		DelegationSuggester suggester = new DelegationSuggester((userA, userB) -> false);

		assertThat(suggester.suggest(List.of(
				debt(1, 10, 20, "400.00"),
				debt(2, 20, 30, "350.00"))))
				.isEmpty();
	}

	private static OpenDebt debt(long id, long debtor, long creditor, String remaining) {
		return new OpenDebt(id, debtor, creditor, money(remaining));
	}

	private static BigDecimal money(String amount) {
		return new BigDecimal(amount);
	}
}
