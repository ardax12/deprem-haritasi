package com.ag.deprem.service;

import com.ag.deprem.model.dto.AfadRawEventDto;
import com.ag.deprem.model.entity.SourceReport;
import com.ag.deprem.model.entity.SourceReportRevision;
import com.ag.deprem.repository.SourceReportRepository;
import com.ag.deprem.repository.SourceReportRevisionRepository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class IngestionServiceTest {

    @Autowired
    private IngestionService ingestionService;

    @Autowired
    private SourceReportRepository reportRepository;

    @Autowired
    private SourceReportRevisionRepository revisionRepository;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        reportRepository.deleteAll();
        objectMapper = new ObjectMapper();
    }

    @Test
    void testIngestSampleEventsAndIdempotency() throws Exception {
        List<AfadRawEventDto> samples;
        try (InputStream is = getClass().getResourceAsStream("/fixtures/afad/afad_sample_events.json")) {
            samples = objectMapper.readValue(is, new TypeReference<>() {});
        }

        // First run: 2 records should be inserted
        IngestionService.IngestionSummary firstRun = ingestionService.ingestAfadEvents(samples);
        assertEquals(2, firstRun.getTotal());
        assertEquals(2, firstRun.getInserted());
        assertEquals(0, firstRun.getUpdated());
        assertEquals(0, firstRun.getUnchanged());
        assertEquals(0, firstRun.getRejected());

        assertEquals(2, reportRepository.count());
        assertEquals(2, revisionRepository.count());

        Optional<SourceReport> reportOpt = reportRepository.findBySourceAndSourceEventId("AFAD", "731199");
        assertTrue(reportOpt.isPresent());
        SourceReport report = reportOpt.get();
        assertNotNull(report.getCurrentRevisionId());

        Optional<SourceReportRevision> revOpt = revisionRepository.findById(report.getCurrentRevisionId());
        assertTrue(revOpt.isPresent());
        assertEquals(1, revOpt.get().getRevisionNumber());
        assertEquals(2.1, revOpt.get().getMagnitudeValue());

        // Second run with EXACT SAME DATA: should be unchanged (no duplicates created!)
        IngestionService.IngestionSummary secondRun = ingestionService.ingestAfadEvents(samples);
        assertEquals(2, secondRun.getTotal());
        assertEquals(0, secondRun.getInserted());
        assertEquals(0, secondRun.getUpdated());
        assertEquals(2, secondRun.getUnchanged());
        assertEquals(0, secondRun.getRejected());

        assertEquals(2, reportRepository.count(), "Report count must remain 2");
        assertEquals(2, revisionRepository.count(), "Revision count must remain 2");
    }

    @Test
    void testIngestUpdatedRevisionPreservesOldRevision() throws Exception {
        List<AfadRawEventDto> samples;
        List<AfadRawEventDto> updated;

        try (InputStream is1 = getClass().getResourceAsStream("/fixtures/afad/afad_sample_events.json");
             InputStream is2 = getClass().getResourceAsStream("/fixtures/afad/afad_updated_event.json")) {
            samples = objectMapper.readValue(is1, new TypeReference<>() {});
            updated = objectMapper.readValue(is2, new TypeReference<>() {});
        }

        // 1. Initial ingestion
        ingestionService.ingestAfadEvents(samples);

        // 2. Ingest update for event 731199
        IngestionService.IngestionSummary updateRun = ingestionService.ingestAfadEvents(updated);
        assertEquals(1, updateRun.getTotal());
        assertEquals(0, updateRun.getInserted());
        assertEquals(1, updateRun.getUpdated());

        SourceReport report = reportRepository.findBySourceAndSourceEventId("AFAD", "731199").orElseThrow();
        assertTrue(report.getIsEventUpdate());

        List<SourceReportRevision> allRevs = revisionRepository.findBySourceReportIdOrderByRevisionNumberDesc(report.getId());
        assertEquals(2, allRevs.size(), "Both revisions must be preserved in history");

        SourceReportRevision latestRev = allRevs.get(0);
        SourceReportRevision previousRev = allRevs.get(1);

        assertEquals(2, latestRev.getRevisionNumber());
        assertEquals(2.3, latestRev.getMagnitudeValue());
        assertEquals("Mw", latestRev.getMagnitudeType());
        assertEquals(report.getCurrentRevisionId(), latestRev.getId());

        assertEquals(1, previousRev.getRevisionNumber());
        assertEquals(2.1, previousRev.getMagnitudeValue());
        assertEquals("ML", previousRev.getMagnitudeType());
    }

    @Test
    void testRejectsInvalidEventsWithoutFailingBatch() throws Exception {
        List<AfadRawEventDto> invalidList;
        try (InputStream is = getClass().getResourceAsStream("/fixtures/afad/afad_invalid_events.json")) {
            invalidList = objectMapper.readValue(is, new TypeReference<>() {});
        }

        IngestionService.IngestionSummary summary = ingestionService.ingestAfadEvents(invalidList);
        assertEquals(2, summary.getTotal());
        assertEquals(2, summary.getRejected());
        assertEquals(0, summary.getInserted());
        assertEquals(0, reportRepository.count());
    }

    @Test
    void testAcceptsMissingMagnitudeWithValidCoords() throws Exception {
        List<AfadRawEventDto> missingMagList;
        try (InputStream is = getClass().getResourceAsStream("/fixtures/afad/afad_missing_magnitude_event.json")) {
            missingMagList = objectMapper.readValue(is, new TypeReference<>() {});
        }

        IngestionService.IngestionSummary summary = ingestionService.ingestAfadEvents(missingMagList);
        assertEquals(1, summary.getInserted());
        assertEquals(0, summary.getRejected());

        SourceReport report = reportRepository.findBySourceAndSourceEventId("AFAD", "731299").orElseThrow();
        SourceReportRevision rev = revisionRepository.findById(report.getCurrentRevisionId()).orElseThrow();
        assertNull(rev.getMagnitudeValue());
        assertEquals(39.0, rev.getLatitude());
        assertEquals(35.0, rev.getLongitude());
    }
}
