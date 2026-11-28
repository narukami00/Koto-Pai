package com.kotopai.admin.config;

import java.util.Locale;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.kotopai.user.domain.Role;
import com.kotopai.user.domain.User;
import com.kotopai.user.repository.UserRepository;

/**
 * Makes sure a real administrator exists, driven entirely by configuration, so a production deployment never
 * depends on a demo account with a published password.
 *
 * <p>Settings (environment variables, all optional): {@code KOTOPAI_ADMIN_USER_CODE},
 * {@code KOTOPAI_ADMIN_EMAIL}, {@code KOTOPAI_ADMIN_PASSWORD}, {@code KOTOPAI_ADMIN_DISPLAY_NAME}.
 *
 * <ul>
 *   <li>No user code configured: nothing happens.</li>
 *   <li>The user already exists: they are promoted to ADMIN if they are not one yet. Their password is never
 *       touched.</li>
 *   <li>The user does not exist: an ADMIN account is created from the e-mail and password settings.</li>
 * </ul>
 *
 * <p>The runner is idempotent (safe on every start), never logs the password, and never stops the application
 * from starting: a bad configuration is logged and skipped.
 */
@Component
@Order(2)
public class AdminBootstrapRunner implements CommandLineRunner {

    static final int MIN_PASSWORD_LENGTH = 8;

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String userCode;
    private final String email;
    private final String password;
    private final String displayName;

    public AdminBootstrapRunner(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${kotopai.admin.user-code:}") String userCode,
            @Value("${kotopai.admin.email:}") String email,
            @Value("${kotopai.admin.password:}") String password,
            @Value("${kotopai.admin.display-name:Administrator}") String displayName) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userCode = clean(userCode);
        this.email = clean(email);
        this.password = password == null ? "" : password;
        this.displayName = clean(displayName).isEmpty() ? "Administrator" : clean(displayName);
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userCode.isEmpty()) {
            log.debug("AdminBootstrapRunner: no KOTOPAI_ADMIN_USER_CODE configured; nothing to do.");
            return;
        }
        String code = userCode.toLowerCase(Locale.ROOT);

        Optional<User> existing = userRepository.findByUserCode(code);
        if (existing.isPresent()) {
            promoteIfNeeded(existing.get());
            return;
        }
        createAdmin(code);
    }

    private void promoteIfNeeded(User user) {
        if (user.getRole() == Role.ADMIN) {
            log.info("AdminBootstrapRunner: '{}' is already an administrator.", user.getUserCode());
            return;
        }
        user.changeRole(Role.ADMIN);
        userRepository.save(user);
        log.info("AdminBootstrapRunner: promoted existing user '{}' to administrator.", user.getUserCode());
    }

    private void createAdmin(String code) {
        if (email.isEmpty() || password.length() < MIN_PASSWORD_LENGTH) {
            log.warn("AdminBootstrapRunner: user '{}' does not exist and cannot be created. Set KOTOPAI_ADMIN_EMAIL and "
                    + "a KOTOPAI_ADMIN_PASSWORD of at least {} characters, or register '{}' first.",
                    code, MIN_PASSWORD_LENGTH, code);
            return;
        }
        if (userRepository.existsByEmail(email.toLowerCase(Locale.ROOT))) {
            log.warn("AdminBootstrapRunner: the configured e-mail already belongs to another user; "
                    + "administrator '{}' was not created.", code);
            return;
        }
        try {
            userRepository.save(new User(code, displayName, email, passwordEncoder.encode(password), Role.ADMIN));
            log.info("AdminBootstrapRunner: created administrator '{}'.", code);
        } catch (IllegalArgumentException invalid) {
            log.error("AdminBootstrapRunner: administrator '{}' was not created: {}", code, invalid.getMessage());
        }
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
