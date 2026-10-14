package com.ag.deprem.service;

import com.ag.deprem.model.dto.EarthquakeDetailDto;
import com.ag.deprem.model.dto.EarthquakesListResponseDto;
import com.ag.deprem.model.dto.KoeriRawEventDto;
import com.ag.deprem.model.dto.StatusResponseDto;
import com.ag.deprem.model.entity.SourceReport;
import com.ag.deprem.model.entity.SourceReportRevision;
import com.ag.deprem.repository.SourceReportRepository;
import com.ag.deprem.repository.SourceReportRevisionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class KoeriIngestionIntegrationTest {

    @Autowired
    private IngestionService ingestionService;

    @Autowired
    private KoeriPageParser parser;

    @Autowired
    private SourceReportRepository reportRepository;

    @Autowired
    private SourceReportRevisionRepository revisionRepository;

    @Autowired
    private EarthquakeQueryService queryService;

    @Autowired
    private SourceStatusTracker statusTracker;

    @BeforeEach
    void setUp() {
        reportRepository.deleteAll();
    }

    @Test
    void testIngestKoeriFixtureAndQuery() throws Exception {
        byte[] bytes;
        try (InputStream is = getClass().getResourceAsStream("/fixtures/koeri/koeri_sample_lst0.html")) {
            assertNotNull(is);
            bytes = is.readAllBytes();
        }

        KoeriPageParser.ParseResult parseResult = parser.parse(bytes, StandardCharsets.UTF_8);
        List<KoeriRawEventDto> rawEvents = parseResult.getEvents();
        assertEquals(10, rawEvents.size());

        // Ingest KOERI events
        IngestionService.IngestionSummary summary = ingestionService.ingestKoeriEvents(rawEvents);
        assertEquals(10, summary.getTotal());
        assertEquals(10, summary.getInserted());
        assertEquals(0, summary.getRejected());

        // Verify database records
        assertEquals(10, reportRepository.count());
        assertEquals(10, revisionRepository.count());

        // Check one event
        List<SourceReport> allReports = reportRepository.findAll();
        SourceReport firstRep = allReports.get(0);
        assertEquals("KOERI", firstRep.getSource());
        assertTrue(firstRep.getSourceEventId().startsWith("koeri-"));

        SourceReportRevision rev = revisionRepository.findById(firstRep.getCurrentRevisionId()).orElseThrow();
        assertNotNull(rev.getSolutionQuality());

        // Query earthquakes with source filter
        Instant start = Instant.parse("2026-10-14T00:00:00Z");
        Instant end = Instant.parse("2026-10-14T23:59:59Z");

        EarthquakesListResponseDto koeriList = queryService.queryEarthquakes(
                start, end, null, null, null, null, null, null, null, "KOERI", 0, 50, "occurredAt:desc"
        );
        assertEquals(10, koeriList.getItems().size());
        assertEquals("KOERI", koeriList.getItems().get(0).getSource());

        // Query earthquakes with source filter AFAD -> should be 0
        EarthquakesListResponseDto afadList = queryService.queryEarthquakes(
                start, end, null, null, null, null, null, null, null, "AFAD", 0, 50, "occurredAt:desc"
        );
        assertEquals(0, afadList.getItems().size());

        // Detail lookup using koeri:<eventId>
        String detailId = "koeri:" + firstRep.getSourceEventId();
        Optional<EarthquakeDetailDto> detailOpt = queryService.getEarthquakeDetail(detailId);
        assertTrue(detailOpt.isPresent());
        assertEquals("KOERI", detailOpt.get().getCurrent().getSource());

        // Check StatusResponseDto contains both AFAD and KOERI
        StatusResponseDto status = queryService.getStatus();
        assertNotNull(status);
        assertEquals(2, status.getSources().size());
        assertTrue(status.getSources().stream().anyMatch(s -> s.getName().equals("AFAD")));
        assertTrue(status.getSources().stream().anyMatch(s -> s.getName().equals("KOERI")));
    }
}
