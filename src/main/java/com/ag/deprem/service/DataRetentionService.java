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
    private final int retentionDays;

    public DataRetentionService(SourceReportRepository sourceReportRepository,
                                @Value("${app.retention.days:30}") int retentionDays) {
        this.sourceReportRepository = sourceReportRepository;
        this.retentionDays = retentionDays;
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
