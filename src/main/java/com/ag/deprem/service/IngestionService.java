package com.ag.deprem.service;

import com.ag.deprem.model.dto.AfadRawEventDto;
import com.ag.deprem.model.dto.NormalizedReportDto;
import com.ag.deprem.model.entity.SourceReport;
import com.ag.deprem.model.entity.SourceReportRevision;
import com.ag.deprem.repository.SourceReportRepository;
import com.ag.deprem.repository.SourceReportRevisionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    private final SourceReportRepository sourceReportRepository;
    private final SourceReportRevisionRepository revisionRepository;
    private final AfadDataNormalizer normalizer;

    public IngestionService(SourceReportRepository sourceReportRepository,
                            SourceReportRevisionRepository revisionRepository,
                            AfadDataNormalizer normalizer) {
        this.sourceReportRepository = sourceReportRepository;
        this.revisionRepository = revisionRepository;
        this.normalizer = normalizer;
    }

    public static class IngestionSummary {
        private int total;
        private int inserted;
        private int updated;
        private int unchanged;
        private int rejected;

        public int getTotal() { return total; }
        public int getInserted() { return inserted; }
        public int getUpdated() { return updated; }
        public int getUnchanged() { return unchanged; }
        public int getRejected() { return rejected; }

        @Override
        public String toString() {
            return String.format("total=%d, inserted=%d, updated=%d, unchanged=%d, rejected=%d",
                    total, inserted, updated, unchanged, rejected);
        }
    }

    @Transactional
    public IngestionSummary ingestAfadEvents(List<AfadRawEventDto> rawEvents) {
        IngestionSummary summary = new IngestionSummary();
        if (rawEvents == null || rawEvents.isEmpty()) {
            return summary;
        }

        Instant now = Instant.now();

        for (AfadRawEventDto raw : rawEvents) {
            summary.total++;
            NormalizedReportDto dto = normalizer.normalize(raw);

            if (dto == null || !dto.isValid()) {
                summary.rejected++;
                log.warn("Rejected invalid AFAD record: ID='{}', reasons={}",
                        raw != null ? raw.getEventId() : "null",
                        dto != null ? dto.getDataQualityNotes() : "null");
                continue;
            }

            Optional<SourceReport> existingOpt = sourceReportRepository
                    .findBySourceAndSourceEventId(dto.getSource(), dto.getSourceEventId());

            if (existingOpt.isEmpty()) {
                // New Source Report
                SourceReport report = new SourceReport();
                report.setId(UUID.randomUUID());
                report.setSource(dto.getSource());
                report.setSourceEventId(dto.getSourceEventId());
                report.setFirstSeenAt(now);
                report.setLastSeenAt(now);
                report.setIsEventUpdate(dto.getIsEventUpdate());
                report.setSourceUpdatedAt(dto.getSourceUpdatedAt());

                SourceReportRevision revision = createRevision(report, dto, 1, now);
                report.setCurrentRevisionId(revision.getId());

                sourceReportRepository.save(report);
                revisionRepository.save(revision);
                summary.inserted++;
            } else {
                SourceReport report = existingOpt.get();
                report.setLastSeenAt(now);

                Optional<SourceReportRevision> latestRevOpt = revisionRepository
                        .findFirstBySourceReportIdOrderByRevisionNumberDesc(report.getId());

                if (latestRevOpt.isPresent() &&
                        latestRevOpt.get().getNormalizedHash().equals(dto.getNormalizedHash())) {
                    // Content identical -> no new revision
                    sourceReportRepository.save(report);
                    summary.unchanged++;
                } else {
                    // Content changed -> create new revision
                    int nextRevNumber = latestRevOpt.map(r -> r.getRevisionNumber() + 1).orElse(1);
                    SourceReportRevision newRevision = createRevision(report, dto, nextRevNumber, now);

                    report.setCurrentRevisionId(newRevision.getId());
                    report.setIsEventUpdate(true);
                    if (dto.getSourceUpdatedAt() != null) {
                        report.setSourceUpdatedAt(dto.getSourceUpdatedAt());
                    }

                    sourceReportRepository.save(report);
                    revisionRepository.save(newRevision);
                    summary.updated++;
                }
            }
        }

        log.info("AFAD ingestion completed: {}", summary);
        return summary;
    }

    private SourceReportRevision createRevision(SourceReport report, NormalizedReportDto dto, int revNumber, Instant now) {
        SourceReportRevision rev = new SourceReportRevision();
        rev.setId(UUID.randomUUID());
        rev.setSourceReport(report);
        rev.setRevisionNumber(revNumber);
        rev.setOccurredAt(dto.getOccurredAt());
        rev.setLatitude(dto.getLatitude());
        rev.setLongitude(dto.getLongitude());
        rev.setDepthKm(dto.getDepthKm());
        rev.setMagnitudeValue(dto.getMagnitudeValue());
        rev.setMagnitudeType(dto.getMagnitudeType());
        rev.setLocationText(dto.getLocationText());
        rev.setCountry(dto.getCountry());
        rev.setProvince(dto.getProvince());
        rev.setDistrict(dto.getDistrict());
        rev.setNeighborhood(dto.getNeighborhood());
        rev.setSourcePayloadHash(dto.getSourcePayloadHash());
        rev.setNormalizedHash(dto.getNormalizedHash());
        rev.setObservedAt(now);
        return rev;
    }
}
