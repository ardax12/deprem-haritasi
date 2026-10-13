package com.ag.deprem.service;

import com.ag.deprem.model.dto.AfadRawEventDto;
import com.ag.deprem.model.dto.BackfillResultDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class AfadBackfillService {

    private static final Logger log = LoggerFactory.getLogger(AfadBackfillService.class);

    private final AfadApiClient apiClient;
    private final IngestionService ingestionService;
    private final int chunkHours;
    private final long delayMs;
    private final boolean backfillEnabled;

    public AfadBackfillService(AfadApiClient apiClient,
                               IngestionService ingestionService,
                               @Value("${afad.backfill.chunk-hours:24}") int chunkHours,
                               @Value("${afad.backfill.delay-ms:500}") long delayMs,
                               @Value("${afad.backfill.enabled:false}") boolean backfillEnabled) {
        this.apiClient = apiClient;
        this.ingestionService = ingestionService;
        this.chunkHours = Math.max(1, chunkHours);
        this.delayMs = Math.max(0, delayMs);
        this.backfillEnabled = backfillEnabled;
    }

    /**
     * Executes backfill for a given date range, breaking it down into rate-limited polite chunks.
     */
    public BackfillResultDto runBackfill(Instant start, Instant end) {
        if (start == null || end == null || !start.isBefore(end)) {
            throw new IllegalArgumentException("Parameter 'start' must be strictly before 'end'");
        }

        log.info("Starting AFAD historical backfill from {} to {} in {}-hour chunks", start, end, chunkHours);

        int totalFetched = 0;
        int inserted = 0;
        int updated = 0;
        int unchanged = 0;
        int rejected = 0;
        int chunksProcessed = 0;

        Instant currentChunkStart = start;
        Duration chunkDuration = Duration.ofHours(chunkHours);

        while (currentChunkStart.isBefore(end)) {
            Instant currentChunkEnd = currentChunkStart.plus(chunkDuration);
            if (currentChunkEnd.isAfter(end)) {
                currentChunkEnd = end;
            }

            try {
                log.info("Backfilling chunk #{}: [{} to {}]", chunksProcessed + 1, currentChunkStart, currentChunkEnd);
                List<AfadRawEventDto> rawEvents = apiClient.fetchEvents(currentChunkStart, currentChunkEnd);
                totalFetched += rawEvents.size();

                IngestionService.IngestionSummary summary = ingestionService.ingestAfadEvents(rawEvents);
                inserted += summary.getInserted();
                updated += summary.getUpdated();
                unchanged += summary.getUnchanged();
                rejected += summary.getRejected();
                chunksProcessed++;

                // Polite delay between requests to prevent overloading AFAD API
                if (currentChunkEnd.isBefore(end) && delayMs > 0) {
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.warn("Backfill interrupted during delay sleep");
                        break;
                    }
                }
            } catch (Exception e) {
                log.error("Error backfilling chunk [{} to {}]: {}", currentChunkStart, currentChunkEnd, e.getMessage(), e);
            }

            currentChunkStart = currentChunkEnd;
        }

        log.info("AFAD backfill finished: processed {} chunks, {} events fetched (inserted: {}, updated: {}, unchanged: {}, rejected: {})",
                chunksProcessed, totalFetched, inserted, updated, unchanged, rejected);

        return new BackfillResultDto(
                start,
                end,
                totalFetched,
                inserted,
                updated,
                unchanged,
                rejected,
                chunksProcessed,
                true,
                "Tarihsel katalog taraması başarıyla tamamlandı."
        );
    }

    /**
     * Hourly or daily scheduled background backfill (when enabled by configuration).
     */
    @Scheduled(cron = "${afad.backfill.cron:0 0 4 * * *}")
    public void scheduledDailyBackfill() {
        if (!backfillEnabled) {
            return;
        }
        // Safely scan the last 30 to 31 days to ensure historical consistency
        Instant now = Instant.now();
        Instant end = now.minus(30, ChronoUnit.DAYS);
        Instant start = end.minus(chunkHours, ChronoUnit.HOURS);
        log.info("Executing scheduled backfill task for [{} to {}]", start, end);
        runBackfill(start, end);
    }
}
