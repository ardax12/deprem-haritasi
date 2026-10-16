package com.ag.deprem.repository;

import com.ag.deprem.model.entity.FeltReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface FeltReportRepository extends JpaRepository<FeltReport, UUID> {

    boolean existsBySourceReportIdAndIpHash(UUID sourceReportId, String ipHash);

    List<FeltReport> findBySourceReportId(UUID sourceReportId);

    List<FeltReport> findAllBySourceReportIdIn(Collection<UUID> sourceReportIds);

    @Query("SELECT r FROM FeltReport r WHERE r.sourceReport.id IN :reportIds ORDER BY r.reportedAt DESC")
    List<FeltReport> findReportsForReports(@Param("reportIds") Collection<UUID> reportIds);
}
