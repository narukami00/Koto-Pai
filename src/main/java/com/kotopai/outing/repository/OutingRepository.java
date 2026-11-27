package com.kotopai.outing.repository;

import com.kotopai.outing.domain.Outing;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository pattern: the rest of the application asks for outings through this interface and never writes
 * SQL or touches the EntityManager, so persistence details can change in one place.
 */
public interface OutingRepository extends JpaRepository<Outing, Long> {

    /**
     * Outings a user may see, newest first: everything they created (drafts included) plus outings they take
     * part in once those are no longer drafts. Pass a {@link Pageable} to take only the most recent ones.
     */
    @Query("""
        select distinct o from Outing o left join o.participants p
        where o.creatorId = :userId
           or (p.userId = :userId and o.status <> com.kotopai.outing.domain.OutingStatus.DRAFT)
        order by o.date desc, o.id desc
        """)
    List<Outing> findVisibleTo(@Param("userId") Long userId, Pageable pageable);

    /** Loads an outing with its participants and their guests in one query (no N+1 on the participant list). */
    @EntityGraph(attributePaths = {"participants", "participants.guest"})
    Optional<Outing> findWithParticipantsById(Long id);
}
