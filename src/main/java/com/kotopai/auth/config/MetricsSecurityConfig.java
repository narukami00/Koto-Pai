package com.kotopai.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Locks the Prometheus scrape endpoint behind HTTP Basic with one machine account.
 *
 * <p>Prometheus cannot fill in a login form, so {@code /actuator/prometheus} gets its own filter chain that is
 * evaluated before the form-login chain in {@link SecurityConfig}. The scrape account lives only in this chain
 * (it is not a user of the app and cannot log in anywhere else). When no password is configured the endpoint
 * is closed to everybody, so a deployment that forgot the secret never exposes its metrics.
 */
@Configuration
public class MetricsSecurityConfig {

    static final String SCRAPE_ROLE = "METRICS";

    @Bean
    @Order(1)
    SecurityFilterChain metricsFilterChain(
            HttpSecurity http,
            PasswordEncoder passwordEncoder,
            @Value("${kotopai.metrics.username:prometheus}") String username,
            @Value("${kotopai.metrics.password:}") String password) throws Exception {

        http.securityMatcher("/actuator/prometheus")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        if (password == null || password.isBlank()) {
            http.authorizeHttpRequests(auth -> auth.anyRequest().denyAll());
        } else {
            http.authorizeHttpRequests(auth -> auth.anyRequest().hasRole(SCRAPE_ROLE))
                    .authenticationManager(scrapeAuthenticationManager(passwordEncoder, username, password))
                    .httpBasic(Customizer.withDefaults());
        }
        return http.build();
    }

    private static AuthenticationManager scrapeAuthenticationManager(
            PasswordEncoder passwordEncoder, String username, String password) {
        UserDetails scraper = User.withUsername(username)
                .password(passwordEncoder.encode(password))
                .roles(SCRAPE_ROLE)
                .build();
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(new InMemoryUserDetailsManager(scraper));
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }
}
