package com.kotopai.ledger.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.kotopai.ledger.DebtSimplifier;
import com.kotopai.ledger.spi.OutingSettlementProvider;

@Configuration
public class LedgerConfiguration {

	@Bean
	DebtSimplifier debtSimplifier() {
		return new DebtSimplifier();
	}

	@Bean
	@ConditionalOnMissingBean(OutingSettlementProvider.class)
	OutingSettlementProvider unavailableOutingSettlementProvider() {
		return outingId -> {
			throw new IllegalStateException(
					"Member 2 must provide an OutingSettlementProvider before debts can be generated");
		};
	}
}
