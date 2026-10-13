package com.ag.deprem.service;

import com.ag.deprem.model.dto.AfadRawEventDto;
import com.ag.deprem.model.dto.BackfillResultDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AfadBackfillServiceTest {

    private AfadApiClient apiClient;
    private IngestionService ingestionService;
    private AfadBackfillService backfillService;

    @BeforeEach
    void setUp() {
        apiClient = Mockito.mock(AfadApiClient.class);
        ingestionService = Mockito.mock(IngestionService.class);

        backfillService = new AfadBackfillService(
                apiClient,
                ingestionService,
                24, // 24-hour chunks
                0,  // no delay in tests
                false
        );
    }

    @Test
    void testRunBackfillChunks() {
        Instant now = Instant.now();
        Instant start = now.minus(72, ChronoUnit.HOURS);
        Instant end = now;

        AfadRawEventDto sample = new AfadRawEventDto();
        sample.setEventId("sample-1");

        when(apiClient.fetchEvents(any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(sample));

        IngestionService.IngestionSummary summary = new IngestionService.IngestionSummary();
        when(ingestionService.ingestAfadEvents(any())).thenReturn(summary);

        BackfillResultDto result = backfillService.runBackfill(start, end);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(3, result.getChunksProcessed(), "72 hours in 24h chunks should produce 3 chunks");
        assertEquals(3, result.getTotalEventsFetched());

        verify(apiClient, times(3)).fetchEvents(any(Instant.class), any(Instant.class));
        verify(ingestionService, times(3)).ingestAfadEvents(any());
    }

    @Test
    void testRunBackfillInvalidDates() {
        Instant now = Instant.now();
        assertThrows(IllegalArgumentException.class, () -> {
            backfillService.runBackfill(now, now.minus(1, ChronoUnit.DAYS));
        });
    }
}
