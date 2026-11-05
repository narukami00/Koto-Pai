package com.kotopai.ledger.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kotopai.ledger.domain.Debt;

public interface DebtRepository extends JpaRepository<Debt, Long> {

	List<Debt> findAllByOutingId(Long outingId);

	List<Debt> findAllByDebtorIdOrCreditorIdOrderByCreatedAtDesc(Long debtorId, Long creditorId);
}
