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

    default Optional<SourceReport> findReportByIdString(String idStr) {
        if (idStr == null || idStr.isBlank()) {
            return Optional.empty();
        }
        String lower = idStr.toLowerCase();
        if (lower.startsWith("afad:")) {
            return findBySourceAndSourceEventId("AFAD", idStr.substring(5));
        } else if (lower.startsWith("koeri:")) {
            return findBySourceAndSourceEventId("KOERI", idStr.substring(6));
        }

        try {
            UUID uuid = UUID.fromString(idStr);
            return findById(uuid);
        } catch (IllegalArgumentException e) {
            Optional<SourceReport> afadOpt = findBySourceAndSourceEventId("AFAD", idStr);
            if (afadOpt.isPresent()) {
                return afadOpt;
            }
            return findBySourceAndSourceEventId("KOERI", idStr);
        }
    }
}
