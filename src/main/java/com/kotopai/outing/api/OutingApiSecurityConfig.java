package com.kotopai.outing.api;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

/**
 * Security for the outing JSON API only: login is required, and an unauthenticated call gets a plain 401 instead of
 * the login-page redirect meant for browsers. It sits next to the application chain (which Member 1 owns) and
 * only matches this feature own URLs. The session login created by that chain is what authenticates these calls.
 */
@Configuration(proxyBeanMethods = false)
public class OutingApiSecurityConfig {

    @Bean
    @Order(1)
    SecurityFilterChain outingApiSecurity(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/outings/**", "/api/guests/**")
            .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
            .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
        return http.build();
    }
}
