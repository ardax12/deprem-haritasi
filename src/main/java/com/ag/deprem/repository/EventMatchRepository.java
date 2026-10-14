package com.ag.deprem.repository;

import com.ag.deprem.model.entity.EventMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventMatchRepository extends JpaRepository<EventMatch, UUID> {

    @Query("SELECT m FROM EventMatch m WHERE (m.leftReport.id = :r1 AND m.rightReport.id = :r2) OR (m.leftReport.id = :r2 AND m.rightReport.id = :r1)")
    Optional<EventMatch> findMatchBetweenReports(@Param("r1") UUID reportId1, @Param("r2") UUID reportId2);

    @Query("SELECT m FROM EventMatch m WHERE m.leftReport.id = :reportId OR m.rightReport.id = :reportId")
    List<EventMatch> findAllByReportId(@Param("reportId") UUID reportId);

    List<EventMatch> findByStatus(String status);
}
