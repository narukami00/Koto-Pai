package com.kotopai.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kotopai.user.domain.User;

/**
 * Data access repository for {@link User} entities.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUserCode(String userCode);

    Optional<User> findByEmail(String email);

    boolean existsByUserCode(String userCode);

    boolean existsByEmail(String email);
}

