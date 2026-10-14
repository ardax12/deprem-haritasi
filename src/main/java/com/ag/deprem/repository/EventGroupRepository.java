package com.ag.deprem.repository;

import com.ag.deprem.model.entity.EventGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventGroupRepository extends JpaRepository<EventGroup, UUID>, JpaSpecificationExecutor<EventGroup> {

    @Query("SELECT g FROM EventGroup g WHERE g.occurredAt >= :start AND g.occurredAt < :end ORDER BY g.occurredAt DESC")
    Page<EventGroup> findBetween(@Param("start") Instant start, @Param("end") Instant end, Pageable pageable);

    @Query("SELECT g FROM EventGroup g JOIN FETCH g.members m WHERE g.id = :id")
    Optional<EventGroup> findByIdWithMembers(@Param("id") UUID id);
}
