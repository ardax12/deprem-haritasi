package com.ag.deprem.repository;

import com.ag.deprem.model.entity.FeltReport;
import org.springframework.data.domain.Pageable;
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

    boolean existsBySourceReportIdInAndIpHash(Collection<UUID> sourceReportIds, String ipHash);

    List<FeltReport> findBySourceReportId(UUID sourceReportId);

    List<FeltReport> findAllBySourceReportIdIn(Collection<UUID> sourceReportIds);

    @Query("SELECT r FROM FeltReport r WHERE r.sourceReport.id IN :reportIds ORDER BY r.reportedAt DESC")
    List<FeltReport> findReportsForReports(@Param("reportIds") Collection<UUID> reportIds);

    @Query("SELECT COUNT(r), COALESCE(AVG(r.calculatedIntensity), 0.0) FROM FeltReport r WHERE r.sourceReport.id IN :reportIds")
    List<Object[]> getIntensitySummary(@Param("reportIds") Collection<UUID> reportIds);

    @Query("SELECT r.province, COUNT(r), COALESCE(AVG(r.calculatedIntensity), 0.0) FROM FeltReport r " +
           "WHERE r.sourceReport.id IN :reportIds AND r.province IS NOT NULL AND TRIM(r.province) != '' " +
           "GROUP BY r.province ORDER BY COUNT(r) DESC")
    List<Object[]> findProvinceStats(@Param("reportIds") Collection<UUID> reportIds, Pageable pageable);

    @Query("SELECT r.reaction, COUNT(r) FROM FeltReport r " +
           "WHERE r.sourceReport.id IN :reportIds AND r.reaction IS NOT NULL AND TRIM(r.reaction) != '' " +
           "GROUP BY r.reaction ORDER BY COUNT(r) DESC")
    List<Object[]> findDominantReaction(@Param("reportIds") Collection<UUID> reportIds, Pageable pageable);
}
