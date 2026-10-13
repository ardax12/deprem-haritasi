package com.ag.deprem.repository;

import com.ag.deprem.model.entity.SourceReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SourceReportRepository extends JpaRepository<SourceReport, UUID> {
    Optional<SourceReport> findBySourceAndSourceEventId(String source, String sourceEventId);
    List<SourceReport> findByFirstSeenAtBefore(Instant cutoff);
}
