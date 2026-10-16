package com.ag.deprem.service;

import com.ag.deprem.model.dto.ArchiveManifestDto;
import com.ag.deprem.model.entity.SourceReport;
import com.ag.deprem.model.entity.SourceReportRevision;
import com.ag.deprem.repository.SourceReportRepository;
import com.ag.deprem.repository.SourceReportRevisionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(name = "app.retention.enabled", havingValue = "true", matchIfMissing = true)
public class DataRetentionService {

    private static final Logger log = LoggerFactory.getLogger(DataRetentionService.class);

    private final SourceReportRepository sourceReportRepository;
    private final SourceReportRevisionRepository revisionRepository;
    private final ArchiveExporterService archiveExporterService;
    private final com.ag.deprem.repository.EventGroupRepository eventGroupRepository;
    private final int retentionDays;
    private final boolean autoExportBeforeCleanup;
    private final boolean dryRun;

    public DataRetentionService(SourceReportRepository sourceReportRepository,
                                SourceReportRevisionRepository revisionRepository,
                                ArchiveExporterService archiveExporterService,
                                com.ag.deprem.repository.EventGroupRepository eventGroupRepository,
                                @Value("${app.retention.days:30}") int retentionDays,
                                @Value("${app.archive.auto-export-before-cleanup:true}") boolean autoExportBeforeCleanup,
                                @Value("${app.retention.dry-run:false}") boolean dryRun) {
        this.sourceReportRepository = sourceReportRepository;
        this.revisionRepository = revisionRepository;
        this.archiveExporterService = archiveExporterService;
        this.eventGroupRepository = eventGroupRepository;
        this.retentionDays = retentionDays;
        this.autoExportBeforeCleanup = autoExportBeforeCleanup;
        this.dryRun = dryRun;
    }

    // Run every day at 03:00 UTC
    @Scheduled(cron = "${app.retention.cron:0 0 3 * * *}")
    @Transactional
    public void cleanupOldReports() {
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        log.info("Running data retention check for events occurred before {} ({} days retention window, dryRun={})",
                cutoff, retentionDays, dryRun);

        try {
            List<SourceReportRevision> expiredRevisions = revisionRepository.findCurrentRevisionsOccurredBefore(cutoff);
            if (expiredRevisions.isEmpty()) {
                log.info("Data retention check: no expired events found prior to {}", cutoff);
                return;
            }

            Set<SourceReport> oldReportsSet = expiredRevisions.stream()
                    .map(SourceReportRevision::getSourceReport)
                    .collect(Collectors.toSet());
            List<SourceReport> oldReports = List.copyOf(oldReportsSet);

            Instant minOccurredAt = expiredRevisions.stream()
                    .map(SourceReportRevision::getOccurredAt)
                    .min(Instant::compareTo)
                    .orElse(cutoff.minus(7, ChronoUnit.DAYS));

            if (dryRun) {
                log.info("Data retention [DRY-RUN]: Would export & purge {} reports ({} revisions) from {} to {}. Skipping deletion.",
                        oldReports.size(), expiredRevisions.size(), minOccurredAt, cutoff);
                return;
            }

            if (autoExportBeforeCleanup) {
                String shardVersion = "retention-" + cutoff.toEpochMilli();
                log.info("Auto-exporting and verifying archive shard before cleanup: {} [{} to {}]",
                        shardVersion, minOccurredAt, cutoff);

                ArchiveManifestDto manifest = archiveExporterService.exportArchive(minOccurredAt, cutoff, shardVersion);
                boolean verified = archiveExporterService.verifyArchive(shardVersion);

                if (!verified || manifest == null || !Boolean.TRUE.equals(manifest.getVerified())) {
                    log.error("ABORTING RETENTION PURGE: Archive export verification failed for shard '{}'", shardVersion);
                    throw new IllegalStateException("Retention abort: unverified archive shard " + shardVersion);
                }

                log.info("Archive shard '{}' verified successfully (records: {}, checksum: {}). Proceeding with database cleanup.",
                        shardVersion, manifest.getRecordCount(), manifest.getChecksum());
            }

            sourceReportRepository.deleteAll(oldReports);
            sourceReportRepository.flush();
            int purgedGroups = eventGroupRepository.deleteEmptyGroups();
            log.info("Data retention cleanup completed: safely removed {} old reports, their revisions, and {} empty event groups.",
                    oldReports.size(), purgedGroups);
        } catch (Exception e) {
            log.error("Error during data retention cleanup: {}", e.getMessage(), e);
        }
    }
}
