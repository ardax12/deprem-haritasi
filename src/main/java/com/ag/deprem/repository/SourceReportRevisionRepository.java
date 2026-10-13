package com.ag.deprem.repository;

import com.ag.deprem.model.entity.SourceReportRevision;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SourceReportRevisionRepository extends JpaRepository<SourceReportRevision, UUID>, JpaSpecificationExecutor<SourceReportRevision> {

    List<SourceReportRevision> findBySourceReportIdOrderByRevisionNumberDesc(UUID sourceReportId);

    Optional<SourceReportRevision> findFirstBySourceReportIdOrderByRevisionNumberDesc(UUID sourceReportId);

    @Query("SELECT r FROM SourceReportRevision r WHERE r.id = r.sourceReport.currentRevisionId AND r.occurredAt >= :start AND r.occurredAt < :end ORDER BY r.occurredAt DESC")
    Page<SourceReportRevision> findCurrentRevisionsBetween(@Param("start") Instant start, @Param("end") Instant end, Pageable pageable);

    @Query("SELECT r FROM SourceReportRevision r JOIN FETCH r.sourceReport WHERE r.id = r.sourceReport.currentRevisionId AND r.occurredAt >= :start AND r.occurredAt < :end ORDER BY r.occurredAt ASC")
    List<SourceReportRevision> findAllCurrentRevisionsBetween(@Param("start") Instant start, @Param("end") Instant end);

    @Query("SELECT MAX(r.occurredAt) FROM SourceReportRevision r")
    Optional<Instant> findLatestOccurredAt();

    @Query("SELECT MIN(r.occurredAt) FROM SourceReportRevision r")
    Optional<Instant> findEarliestOccurredAt();

    @Query("SELECT r FROM SourceReportRevision r JOIN FETCH r.sourceReport WHERE r.id = r.sourceReport.currentRevisionId AND r.occurredAt < :cutoff ORDER BY r.occurredAt ASC")
    List<SourceReportRevision> findCurrentRevisionsOccurredBefore(@Param("cutoff") Instant cutoff);
}
