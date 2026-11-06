package com.kotopai.ledger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Finds every safe largest-amount delegation in a collection of open debts. */
public final class DelegationSuggester {

	private final FriendshipLookup friendships;

	public DelegationSuggester(FriendshipLookup friendships) {
		this.friendships = Objects.requireNonNull(friendships, "friendships must not be null");
	}

	public List<DelegationSuggestion> suggest(List<OpenDebt> debts) {
		Objects.requireNonNull(debts, "debts must not be null");
		List<DelegationSuggestion> suggestions = new ArrayList<>();

		for (OpenDebt incoming : debts) {
			for (OpenDebt outgoing : debts) {
				long middlemanId = incoming.creditorId();
				if (incoming.id() == outgoing.id()
						|| outgoing.debtorId() != middlemanId
						|| incoming.debtorId() == outgoing.creditorId()
						|| !friendships.areFriends(incoming.debtorId(), outgoing.creditorId())) {
					continue;
				}

				suggestions.add(new DelegationSuggestion(
						incoming.id(),
						outgoing.id(),
						incoming.debtorId(),
						middlemanId,
						outgoing.creditorId(),
						incoming.remaining().min(outgoing.remaining())));
			}
		}

		return List.copyOf(suggestions);
	}
}
