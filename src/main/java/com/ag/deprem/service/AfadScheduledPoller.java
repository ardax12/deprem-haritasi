package com.ag.deprem.service;

import com.ag.deprem.model.dto.AfadRawEventDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@ConditionalOnProperty(name = "afad.sync.enabled", havingValue = "true", matchIfMissing = true)
public class AfadScheduledPoller {

    private static final Logger log = LoggerFactory.getLogger(AfadScheduledPoller.class);

    private final AfadApiClient apiClient;
    private final IngestionService ingestionService;
    private final Duration windowDuration;
    private final AtomicBoolean isPolling = new AtomicBoolean(false);

    public AfadScheduledPoller(AfadApiClient apiClient,
                               IngestionService ingestionService,
                               @Value("${afad.sync.window-hours:24}") int windowHours) {
        this.apiClient = apiClient;
        this.ingestionService = ingestionService;
        this.windowDuration = Duration.ofHours(windowHours);
    }

    // Run every 10 minutes (600,000 ms) with initial delay of 5 seconds
    @Scheduled(fixedDelayString = "${afad.sync.interval-ms:600000}", initialDelay = 5000)
    public void pollRecentEvents() {
        if (!isPolling.compareAndSet(false, true)) {
            log.warn("AFAD sync poller is already running in another thread, skipping execution.");
            return;
        }

        Instant now = Instant.now();
        Instant start = now.minus(windowDuration);
        log.info("Starting scheduled AFAD sync for interval: [{} to {}]", start, now);

        try {
            List<AfadRawEventDto> rawEvents = apiClient.fetchEvents(start, now);
            IngestionService.IngestionSummary summary = ingestionService.ingestAfadEvents(rawEvents);
            log.info("Scheduled AFAD sync finished: {}", summary);
        } catch (Exception e) {
            log.error("Error during scheduled AFAD sync: {}", e.getMessage(), e);
        } finally {
            isPolling.set(false);
        }
    }
}
