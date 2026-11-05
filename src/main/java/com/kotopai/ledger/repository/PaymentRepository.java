package com.kotopai.ledger.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kotopai.ledger.domain.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

	boolean existsByDebtOutingId(Long outingId);
}
