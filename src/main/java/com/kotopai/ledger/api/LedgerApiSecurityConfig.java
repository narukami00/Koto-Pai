package com.kotopai.ledger.api;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

@Configuration(proxyBeanMethods = false)
public class LedgerApiSecurityConfig {

	@Bean
	@Order(1)
	SecurityFilterChain ledgerApiSecurity(HttpSecurity http) throws Exception {
		http
				.securityMatcher(
						"/api/ledger/**", "/api/debts/**", "/api/payments/**", "/api/guest-debts/**",
						"/api/delegations/**", "/api/activity/**")
				.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
				.exceptionHandling(configurer -> configurer
						.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
		return http.build();
	}
}
