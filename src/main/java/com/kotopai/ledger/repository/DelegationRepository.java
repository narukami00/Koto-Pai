package com.kotopai.ledger.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kotopai.ledger.domain.Delegation;

public interface DelegationRepository extends JpaRepository<Delegation, Long> {

	Optional<Delegation> findByDelegatedDebtId(Long delegatedDebtId);
}
