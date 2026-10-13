package com.ag.deprem.service;

import com.ag.deprem.model.dto.AfadRawEventDto;
import com.ag.deprem.model.dto.ArchiveManifestDto;
import com.ag.deprem.repository.ArchiveManifestRepository;
import com.ag.deprem.repository.SourceReportRepository;
import com.ag.deprem.repository.SourceReportRevisionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ArchiveExporterServiceTest {

    @Autowired
    private SourceReportRepository reportRepository;

    @Autowired
    private ArchiveManifestRepository manifestRepository;

    @Autowired
    private IngestionService ingestionService;

    @Autowired
    private SourceReportRevisionRepository revisionRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @TempDir
    Path tempArchiveDir;

    private ArchiveExporterService archiveExporterService;

    @BeforeEach
    void setUp() throws Exception {
        manifestRepository.deleteAll();
        reportRepository.deleteAll();

        // Ingest sample fixture events
        try (InputStream is = getClass().getResourceAsStream("/fixtures/afad/afad_sample_events.json")) {
            List<AfadRawEventDto> samples = objectMapper.readValue(is, new TypeReference<>() {});
            ingestionService.ingestAfadEvents(samples);
        }

        archiveExporterService = new ArchiveExporterService(
                revisionRepository,
                manifestRepository,
                objectMapper,
                tempArchiveDir.toString()
        );
    }

    @Test
    void testExportArchiveAndVerification() {
        Instant now = Instant.now();
        Instant start = now.minus(365, ChronoUnit.DAYS);
        Instant end = now.plus(1, ChronoUnit.DAYS);
        String version = "archive-test-v1";

        ArchiveManifestDto manifest = archiveExporterService.exportArchive(start, end, version);

        assertNotNull(manifest);
        assertEquals(version, manifest.getArchiveVersion());
        assertTrue(manifest.getRecordCount() > 0, "Shard should have exported sample records");
        assertNotNull(manifest.getChecksum());
        assertEquals(64, manifest.getChecksum().length(), "SHA-256 hex checksum should be 64 characters");
        assertTrue(manifest.getVerified());

        // Verify file created on disk
        Path shardFile = tempArchiveDir.resolve(version + ".json");
        assertTrue(Files.exists(shardFile), "Shard JSON file must exist on disk");

        // Verify master manifest.json created on disk
        Path masterManifest = tempArchiveDir.resolve("manifest.json");
        assertTrue(Files.exists(masterManifest), "Master manifest.json must exist on disk");

        // Verify checksum integrity
        boolean verified = archiveExporterService.verifyArchive(version);
        assertTrue(verified, "Archive integrity check should pass");

        // Verify query by version
        assertTrue(archiveExporterService.getManifest(version).isPresent());
        assertEquals(1, archiveExporterService.listManifests().size());
    }
}
