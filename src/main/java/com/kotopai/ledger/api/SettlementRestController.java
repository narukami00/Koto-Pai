package com.kotopai.ledger.api;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kotopai.ledger.api.SettlementDtos.DisputeRequest;
import com.kotopai.ledger.api.SettlementDtos.RecordPaymentRequest;
import com.kotopai.ledger.service.DebtHistoryItem;
import com.kotopai.ledger.service.DisputeService;
import com.kotopai.ledger.service.GuestDebtResult;
import com.kotopai.ledger.service.GuestDebtService;
import com.kotopai.ledger.service.PaymentResult;
import com.kotopai.ledger.service.PaymentService;
import com.kotopai.outing.port.CurrentUserProvider;

@RestController
@RequestMapping("/api")
public class SettlementRestController {

	private final PaymentService payments;
	private final DisputeService disputes;
	private final GuestDebtService guestDebts;
	private final CurrentUserProvider currentUser;

	public SettlementRestController(
			PaymentService payments,
			DisputeService disputes,
			GuestDebtService guestDebts,
			CurrentUserProvider currentUser) {
		this.payments = payments;
		this.disputes = disputes;
		this.guestDebts = guestDebts;
		this.currentUser = currentUser;
	}

	@PostMapping("/debts/{debtId}/payments")
	public ResponseEntity<PaymentResult> recordPayment(
			@PathVariable long debtId,
			@Valid @RequestBody RecordPaymentRequest request) {
		PaymentResult result = payments.record(debtId, currentUser.currentUserId(), request.amount());
		return ResponseEntity.created(URI.create("/api/payments/" + result.id())).body(result);
	}

	@PostMapping("/payments/{paymentId}/confirm")
	public PaymentResult confirmPayment(@PathVariable long paymentId) {
		return payments.confirm(paymentId, currentUser.currentUserId());
	}

	@PostMapping("/debts/{debtId}/dispute")
	public DebtHistoryItem dispute(
			@PathVariable long debtId,
			@Valid @RequestBody DisputeRequest request) {
		return disputes.dispute(debtId, currentUser.currentUserId(), request.reason());
	}

	@GetMapping("/guest-debts")
	public List<GuestDebtResult> guestDebts() {
		return guestDebts.list(currentUser.currentUserId());
	}

	@PostMapping("/guest-debts/{guestDebtId}/settle")
	public GuestDebtResult settleGuestDebt(@PathVariable long guestDebtId) {
		return guestDebts.settle(guestDebtId, currentUser.currentUserId());
	}
}
