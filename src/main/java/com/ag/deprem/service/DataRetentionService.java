package com.ag.deprem.service;

import com.ag.deprem.model.entity.SourceReport;
import com.ag.deprem.repository.SourceReportRepository;
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

@Service
@ConditionalOnProperty(name = "app.retention.enabled", havingValue = "true", matchIfMissing = true)
public class DataRetentionService {

    private static final Logger log = LoggerFactory.getLogger(DataRetentionService.class);

    private final SourceReportRepository sourceReportRepository;
    private final ArchiveExporterService archiveExporterService;
    private final int retentionDays;
    private final boolean autoExportBeforeCleanup;

    public DataRetentionService(SourceReportRepository sourceReportRepository,
                                ArchiveExporterService archiveExporterService,
                                @Value("${app.retention.days:30}") int retentionDays,
                                @Value("${app.archive.auto-export-before-cleanup:true}") boolean autoExportBeforeCleanup) {
        this.sourceReportRepository = sourceReportRepository;
        this.archiveExporterService = archiveExporterService;
        this.retentionDays = retentionDays;
        this.autoExportBeforeCleanup = autoExportBeforeCleanup;
    }

    // Run every day at 03:00 UTC (cron = "0 0 3 * * *") and once 30 seconds after startup
    @Scheduled(cron = "${app.retention.cron:0 0 3 * * *}")
    @Scheduled(initialDelay = 30000, fixedDelay = 86400000)
    @Transactional
    public void cleanupOldReports() {
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        log.info("Running data retention cleanup for reports older than {} ({} days retention)", cutoff, retentionDays);

        try {
            List<SourceReport> oldReports = sourceReportRepository.findByFirstSeenAtBefore(cutoff);
            if (!oldReports.isEmpty()) {
                if (autoExportBeforeCleanup) {
                    try {
                        Instant minDate = oldReports.stream()
                                .map(SourceReport::getFirstSeenAt)
                                .min(Instant::compareTo)
                                .orElse(cutoff.minus(7, ChronoUnit.DAYS));
                        String shardVersion = "retention-" + cutoff.toEpochMilli();
                        log.info("Auto-exporting archive shard before cleanup: {}", shardVersion);
                        archiveExporterService.exportArchive(minDate, cutoff, shardVersion);
                    } catch (Exception ex) {
                        log.warn("Auto-export before cleanup failed: {}", ex.getMessage());
                    }
                }

                sourceReportRepository.deleteAll(oldReports);
                log.info("Data retention cleanup completed: removed {} old reports and their revisions.", oldReports.size());
            } else {
                log.info("Data retention cleanup: no expired reports found.");
            }
        } catch (Exception e) {
            log.error("Error during data retention cleanup: {}", e.getMessage(), e);
        }
    }
}
