package com.ag.deprem.service;

import com.ag.deprem.model.dto.AfadRawEventDto;
import com.ag.deprem.model.dto.KoeriRawEventDto;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    private final SourceReportRepository sourceReportRepository;
    private final SourceReportRevisionRepository revisionRepository;
    private final AfadDataNormalizer afadNormalizer;
    private final KoeriDataNormalizer koeriNormalizer;
    private final EventMatchingService matchingService;

    public IngestionService(SourceReportRepository sourceReportRepository,
                            SourceReportRevisionRepository revisionRepository,
                            AfadDataNormalizer afadNormalizer,
                            KoeriDataNormalizer koeriNormalizer,
                            EventMatchingService matchingService) {
        this.sourceReportRepository = sourceReportRepository;
        this.revisionRepository = revisionRepository;
        this.afadNormalizer = afadNormalizer;
        this.koeriNormalizer = koeriNormalizer;
        this.matchingService = matchingService;
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
        if (rawEvents == null || rawEvents.isEmpty()) {
            return new IngestionSummary();
        }
        List<NormalizedReportDto> normalized = new ArrayList<>();
        IngestionSummary summary = new IngestionSummary();

        for (AfadRawEventDto raw : rawEvents) {
            summary.total++;
            NormalizedReportDto dto = afadNormalizer.normalize(raw);
            if (dto == null || !dto.isValid()) {
                summary.rejected++;
                log.warn("Rejected invalid AFAD record: ID='{}', reasons={}",
                        raw != null ? raw.getEventId() : "null",
                        dto != null ? dto.getDataQualityNotes() : "null");
            } else {
                normalized.add(dto);
            }
        }

        IngestionSummary inner = ingestReports("AFAD", normalized);
        summary.inserted = inner.inserted;
        summary.updated = inner.updated;
        summary.unchanged = inner.unchanged;

        log.info("AFAD ingestion completed: {}", summary);
        return summary;
    }

    @Transactional
    public IngestionSummary ingestKoeriEvents(List<KoeriRawEventDto> rawEvents) {
        if (rawEvents == null || rawEvents.isEmpty()) {
            return new IngestionSummary();
        }
        List<NormalizedReportDto> normalized = new ArrayList<>();
        IngestionSummary summary = new IngestionSummary();

        for (KoeriRawEventDto raw : rawEvents) {
            summary.total++;
            NormalizedReportDto dto = koeriNormalizer.normalize(raw);
            if (dto == null || !dto.isValid()) {
                summary.rejected++;
                log.warn("Rejected invalid KOERI record: line='{}', reasons={}",
                        raw != null ? raw.getRawLine() : "null",
                        dto != null ? dto.getDataQualityNotes() : "null");
            } else {
                normalized.add(dto);
            }
        }

        IngestionSummary inner = ingestReports("KOERI", normalized);
        summary.inserted = inner.inserted;
        summary.updated = inner.updated;
        summary.unchanged = inner.unchanged;

        log.info("KOERI ingestion completed: {}", summary);
        return summary;
    }

    @Transactional
    public IngestionSummary ingestReports(String source, List<NormalizedReportDto> reports) {
        IngestionSummary summary = new IngestionSummary();
        if (reports == null || reports.isEmpty()) {
            return summary;
        }

        Instant now = Instant.now();

        for (NormalizedReportDto dto : reports) {
            summary.total++;
            if (dto == null || !dto.isValid()) {
                summary.rejected++;
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

                try {
                    matchingService.evaluateMatchesForReport(report, revision);
                } catch (Exception e) {
                    log.warn("Matching evaluation failed for new report {}:{}: {}",
                            report.getSource(), report.getSourceEventId(), e.getMessage());
                }
            } else {
                SourceReport report = existingOpt.get();
                report.setLastSeenAt(now);

                Optional<SourceReportRevision> latestRevOpt = revisionRepository
                        .findFirstBySourceReportIdOrderByRevisionNumberDesc(report.getId());

                if (latestRevOpt.isPresent() &&
                        latestRevOpt.get().getNormalizedHash().equals(dto.getNormalizedHash())) {
                    // Content identical -> avoid heavy and redundant database UPDATE
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

                    try {
                        matchingService.handleRevisionChange(report, newRevision);
                    } catch (Exception e) {
                        log.warn("Matching re-evaluation failed for revised report {}:{}: {}",
                                report.getSource(), report.getSourceEventId(), e.getMessage());
                    }
                }
            }
        }

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
        rev.setSolutionQuality(dto.getSolutionQuality());
        rev.setMagMd(dto.getMagMd());
        rev.setMagMl(dto.getMagMl());
        rev.setMagMw(dto.getMagMw());
        return rev;
    }
}
