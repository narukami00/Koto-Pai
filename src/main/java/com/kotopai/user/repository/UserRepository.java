package com.kotopai.user.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kotopai.user.domain.Role;
import com.kotopai.user.domain.User;

/**
 * Data access repository for {@link User} entities.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUserCode(String userCode);

    Optional<User> findByEmail(String email);

    boolean existsByUserCode(String userCode);

    boolean existsByEmail(String email);

    long countByRole(Role role);

    /**
     * Users whose handle, display name or e-mail contains the term, ignoring case, ordered by name. The term must
     * already be lower-cased. {@code locate} is a plain substring test, so characters such as {@code %} and
     * {@code _} in the term are matched literally instead of acting as wildcards.
     */
    @Query("""
            select u from User u
            where locate(:term, lower(u.userCode)) > 0
               or locate(:term, lower(u.displayName)) > 0
               or locate(:term, lower(u.email)) > 0
            order by lower(u.displayName), u.id""")
    List<User> searchByTerm(@Param("term") String term);
}

