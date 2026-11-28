package com.kotopai.user.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import com.kotopai.user.domain.Role;
import com.kotopai.user.domain.User;

/**
 * Persistence integration tests for {@link UserRepository}.
 */
@SpringBootTest
@Transactional
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Saves user and populates generated ID and createdAt timestamp")
    void saveAndFindById() {
        User user = new User("rafsan10", "Rafsan Riasat", "rafsan@example.com", "secret_hash", Role.USER);
        User saved = userRepository.saveAndFlush(user);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();

        Optional<User> found = userRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getUserCode()).isEqualTo("rafsan10");
        assertThat(found.get().getEmail()).isEqualTo("rafsan@example.com");
    }

    @Test
    @DisplayName("Finds user by unique userCode")
    void findByUserCode() {
        User user = new User("tanvir01", "Tanvir Ahmed", "tanvir@example.com", "secret_hash", Role.USER);
        userRepository.saveAndFlush(user);

        Optional<User> found = userRepository.findByUserCode("tanvir01");
        assertThat(found).isPresent();
        assertThat(found.get().getDisplayName()).isEqualTo("Tanvir Ahmed");

        assertThat(userRepository.findByUserCode("nonexistent")).isEmpty();
    }

    @Test
    @DisplayName("Finds user by unique email")
    void findByEmail() {
        User user = new User("nabila02", "Nabila Noor", "nabila@example.com", "secret_hash", Role.USER);
        userRepository.saveAndFlush(user);

        Optional<User> found = userRepository.findByEmail("nabila@example.com");
        assertThat(found).isPresent();
        assertThat(found.get().getUserCode()).isEqualTo("nabila02");

        assertThat(userRepository.findByEmail("nobody@example.com")).isEmpty();
    }

    @Test
    @DisplayName("existsByUserCode and existsByEmail check presence accurately")
    void existsQueries() {
        User user = new User("samiha03", "Samiha Hossain", "samiha@example.com", "secret_hash", Role.USER);
        userRepository.saveAndFlush(user);

        assertThat(userRepository.existsByUserCode("samiha03")).isTrue();
        assertThat(userRepository.existsByUserCode("other")).isFalse();

        assertThat(userRepository.existsByEmail("samiha@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("other@example.com")).isFalse();
    }

    @Test
    @DisplayName("searchByTerm matches handle, display name or e-mail, ignoring case, ordered by name")
    void searchByTermMatchesAnyField() {
        userRepository.saveAndFlush(new User("zqx_carol", "Carol Zqx", "carol@example.com", "hash", Role.USER));
        userRepository.saveAndFlush(new User("zqx_alice", "alice", "alice@example.com", "hash", Role.USER));
        userRepository.saveAndFlush(new User("plain_bob", "Bob", "bob@zqx.example", "hash", Role.USER));
        userRepository.saveAndFlush(new User("unrelated", "Unrelated", "nope@example.com", "hash", Role.USER));

        assertThat(userRepository.searchByTerm("zqx"))
                .extracting(User::getUserCode)
                .containsExactly("zqx_alice", "plain_bob", "zqx_carol");
    }

    @Test
    @DisplayName("searchByTerm treats % and _ as plain characters, never as wildcards")
    void searchByTermIsLiteral() {
        userRepository.saveAndFlush(new User("under_score", "Under Score", "us@example.com", "hash", Role.USER));
        userRepository.saveAndFlush(new User("underxscore", "Underx Score", "ux@example.com", "hash", Role.USER));

        assertThat(userRepository.searchByTerm("er_sc")).extracting(User::getUserCode).containsExactly("under_score");
        assertThat(userRepository.searchByTerm("%")).isEmpty();
    }

    @Test
    @DisplayName("searchByTerm returns an empty list when nothing matches")
    void searchByTermNoMatch() {
        userRepository.saveAndFlush(new User("someone", "Someone", "someone@example.com", "hash", Role.USER));

        assertThat(userRepository.searchByTerm("definitely-not-there")).isEmpty();
    }

    @Test
    @DisplayName("Rejects duplicate userCode via database unique constraint")
    void rejectsDuplicateUserCode() {
        User user1 = new User("duplicate_code", "User One", "user1@example.com", "hash", Role.USER);
        userRepository.saveAndFlush(user1);

        User user2 = new User("duplicate_code", "User Two", "user2@example.com", "hash", Role.USER);
        assertThatThrownBy(() -> userRepository.saveAndFlush(user2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Rejects duplicate email via database unique constraint")
    void rejectsDuplicateEmail() {
        User user1 = new User("code_one", "User One", "shared@example.com", "hash", Role.USER);
        userRepository.saveAndFlush(user1);

        User user2 = new User("code_two", "User Two", "shared@example.com", "hash", Role.USER);
        assertThatThrownBy(() -> userRepository.saveAndFlush(user2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}

