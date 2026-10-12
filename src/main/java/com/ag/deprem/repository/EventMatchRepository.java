package com.ag.deprem.repository;

import com.ag.deprem.model.entity.EventMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface EventMatchRepository extends JpaRepository<EventMatch, UUID> {
}
