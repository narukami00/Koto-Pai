package com.kotopai.ledger.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kotopai.ledger.domain.GuestDebt;
import com.kotopai.ledger.domain.GuestDebtStatus;

public interface GuestDebtRepository extends JpaRepository<GuestDebt, Long> {

	List<GuestDebt> findAllByOutingId(Long outingId);

	List<GuestDebt> findAllByOwnerIdAndStatus(Long ownerId, GuestDebtStatus status);

	void deleteAllByOutingIdAndStatus(Long outingId, GuestDebtStatus status);
}
