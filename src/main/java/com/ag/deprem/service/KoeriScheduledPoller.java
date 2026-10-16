package com.ag.deprem.service;

import com.ag.deprem.model.dto.KoeriRawEventDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@ConditionalOnProperty(name = "koeri.sync.enabled", havingValue = "true", matchIfMissing = true)
public class KoeriScheduledPoller {

    private static final Logger log = LoggerFactory.getLogger(KoeriScheduledPoller.class);

    private final KoeriApiClient apiClient;
    private final KoeriPageParser parser;
    private final IngestionService ingestionService;
    private final SourceStatusTracker statusTracker;
    private final AtomicBoolean isPolling = new AtomicBoolean(false);

    public KoeriScheduledPoller(KoeriApiClient apiClient,
                                KoeriPageParser parser,
                                IngestionService ingestionService,
                                SourceStatusTracker statusTracker) {
        this.apiClient = apiClient;
        this.parser = parser;
        this.ingestionService = ingestionService;
        this.statusTracker = statusTracker;
    }

    @Scheduled(fixedDelayString = "${koeri.sync.interval-ms:600000}", initialDelayString = "${koeri.sync.initial-delay-ms:15000}")
    public void pollRecentEvents() {
        if (!isPolling.compareAndSet(false, true)) {
            log.warn("KOERI sync poller is already running in another thread, skipping execution.");
            return;
        }

        log.info("Starting scheduled KOERI sync from public recent earthquakes page");
        try {
            String pageContent = apiClient.fetchRawPage();
            KoeriPageParser.ParseResult parseResult = parser.parse(pageContent);
            List<KoeriRawEventDto> events = parseResult.getEvents();

            if (events.isEmpty() && parseResult.getTotalLines() > 0) {
                String msg = "KOERI poller retrieved 0 events from page (total lines: " + parseResult.getTotalLines() + ")";
                log.warn(msg);
                statusTracker.recordFailure("KOERI", Instant.now(), msg);
            } else {
                statusTracker.recordSuccess("KOERI", Instant.now());
                if (!events.isEmpty()) {
                    IngestionService.IngestionSummary summary = ingestionService.ingestKoeriEvents(events);
                    log.info("Scheduled KOERI sync finished: parsed={}, summary={}", events.size(), summary);
                }
            }
        } catch (Exception e) {
            log.error("Error during scheduled KOERI sync: {}", e.getMessage(), e);
            statusTracker.recordFailure("KOERI", Instant.now(), e.getMessage());
        } finally {
            isPolling.set(false);
        }
    }
}
