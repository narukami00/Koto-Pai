package com.kotopai.admin.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import com.kotopai.user.domain.Role;
import com.kotopai.user.domain.User;
import com.kotopai.user.repository.UserRepository;

/**
 * Start-up bootstrap against the real context and database: a production deployment configured with the
 * {@code KOTOPAI_ADMIN_*} settings gets its own administrator, with no demo profile involved.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:admin-bootstrap-it;DB_CLOSE_DELAY=-1",
        "kotopai.admin.user-code=boot_admin",
        "kotopai.admin.email=boss@example.com",
        "kotopai.admin.password=Str0ng-passphrase",
        "kotopai.admin.display-name=The Boss"
})
@Transactional
class AdminBootstrapRunnerIT {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AdminBootstrapRunner runner;

    @Test
    @DisplayName("The configured administrator is created at start-up with a hashed password")
    void administratorExistsAfterStartup() {
        User admin = userRepository.findByUserCode("boot_admin").orElseThrow();

        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
        assertThat(admin.getDisplayName()).isEqualTo("The Boss");
        assertThat(admin.getEmail()).isEqualTo("boss@example.com");
        assertThat(admin.getPasswordHash()).isNotEqualTo("Str0ng-passphrase");
        assertThat(passwordEncoder.matches("Str0ng-passphrase", admin.getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("No demo account is created: only the configured administrator exists")
    void noDemoAccountsAreCreated() {
        assertThat(userRepository.findByUserCode("admin")).isEmpty();
        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Running again is harmless: nothing is duplicated and the password is left alone")
    void bootstrapIsIdempotent() {
        String hashBefore = userRepository.findByUserCode("boot_admin").orElseThrow().getPasswordHash();
        long usersBefore = userRepository.count();

        runner.run();
        runner.run();

        assertThat(userRepository.count()).isEqualTo(usersBefore);
        assertThat(userRepository.findByUserCode("boot_admin").orElseThrow().getPasswordHash()).isEqualTo(hashBefore);
    }

    @Test
    @DisplayName("If the configured user was demoted meanwhile, the next start restores the ADMIN role")
    void demotedAdminIsRestoredOnRestart() {
        User admin = userRepository.findByUserCode("boot_admin").orElseThrow();
        admin.changeRole(Role.USER);
        userRepository.saveAndFlush(admin);

        runner.run();

        assertThat(userRepository.findByUserCode("boot_admin").orElseThrow().getRole()).isEqualTo(Role.ADMIN);
    }
}
