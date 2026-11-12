package com.kotopai.friend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kotopai.friend.domain.Friendship;
import com.kotopai.friend.domain.FriendshipStatus;

/**
 * Data access repository for {@link Friendship} relationships.
 */
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    @Query("""
            SELECT f FROM Friendship f
            WHERE (f.requester.id = :userA AND f.addressee.id = :userB)
               OR (f.requester.id = :userB AND f.addressee.id = :userA)
            """)
    Optional<Friendship> findBetween(@Param("userA") Long userA, @Param("userB") Long userB);

    @Query("""
            SELECT COUNT(f) > 0 FROM Friendship f
            WHERE ((f.requester.id = :userA AND f.addressee.id = :userB)
                OR (f.requester.id = :userB AND f.addressee.id = :userA))
              AND f.status = :status
            """)
    boolean existsBetweenWithStatus(
            @Param("userA") Long userA,
            @Param("userB") Long userB,
            @Param("status") FriendshipStatus status);

    @Query("""
            SELECT f FROM Friendship f
            JOIN FETCH f.requester
            JOIN FETCH f.addressee
            WHERE (f.requester.id = :userId OR f.addressee.id = :userId)
              AND f.status = 'ACCEPTED'
            """)
    List<Friendship> findAcceptedForUser(@Param("userId") Long userId);

    @Query("""
            SELECT f FROM Friendship f
            JOIN FETCH f.requester
            WHERE f.addressee.id = :userId
              AND f.status = 'PENDING'
            """)
    List<Friendship> findPendingRequestsForAddressee(@Param("userId") Long userId);
}

