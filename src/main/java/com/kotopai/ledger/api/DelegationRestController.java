package com.kotopai.ledger.api;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kotopai.ledger.DelegationSuggestion;
import com.kotopai.ledger.api.DelegationDtos.ExecuteDelegationRequest;
import com.kotopai.ledger.service.DelegationResult;
import com.kotopai.ledger.service.SettlementFacade;
import com.kotopai.outing.port.CurrentUserProvider;

@RestController
@RequestMapping("/api/delegations")
public class DelegationRestController {

	private final SettlementFacade settlement;
	private final CurrentUserProvider currentUser;

	public DelegationRestController(SettlementFacade settlement, CurrentUserProvider currentUser) {
		this.settlement = settlement;
		this.currentUser = currentUser;
	}

	@GetMapping("/suggestions")
	public List<DelegationSuggestion> suggestions() {
		return settlement.delegationSuggestions(currentUser.currentUserId());
	}

	@PostMapping
	public ResponseEntity<DelegationResult> execute(@Valid @RequestBody ExecuteDelegationRequest request) {
		DelegationResult result = settlement.delegate(
				currentUser.currentUserId(), request.incomingDebtId(), request.outgoingDebtId(), request.amount());
		return ResponseEntity.created(URI.create("/api/delegations/" + result.id())).body(result);
	}
}
