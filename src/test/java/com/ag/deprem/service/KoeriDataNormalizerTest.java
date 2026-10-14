package com.ag.deprem.service;

import com.ag.deprem.model.dto.KoeriRawEventDto;
import com.ag.deprem.model.dto.NormalizedReportDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class KoeriDataNormalizerTest {

    private KoeriDataNormalizer normalizer;

    @BeforeEach
    void setUp() {
        normalizer = new KoeriDataNormalizer();
    }

    @Test
    void testNormalizeValidKoeriEvent() {
        KoeriRawEventDto raw = new KoeriRawEventDto(
                "2026.10.14", "14:39:40", "37.1263", "35.0497", "11.2",
                null, "2.1", null, "BOZCALAR-CUKUROVA (ADANA)", "İlksel", "raw-line"
        );

        NormalizedReportDto dto = normalizer.normalize(raw);

        assertNotNull(dto);
        assertTrue(dto.isValid());
        assertEquals("KOERI", dto.getSource());
        assertTrue(dto.getSourceEventId().startsWith("koeri-"));
        assertTrue(dto.getSourceEventId().length() <= 64, "sourceEventId must fit in VARCHAR(64)");

        // 14:39:40 in Europe/Istanbul (UTC+3) is 11:39:40 UTC
        assertEquals(Instant.parse("2026-10-14T11:39:40Z"), dto.getOccurredAt());

        assertEquals(37.1263, dto.getLatitude());
        assertEquals(35.0497, dto.getLongitude());
        assertEquals(11.2, dto.getDepthKm());
        assertEquals(2.1, dto.getMagnitudeValue());
        assertEquals("ML", dto.getMagnitudeType());
        assertEquals(2.1, dto.getMagMl());
        assertNull(dto.getMagMw());
        assertNull(dto.getMagMd());

        assertEquals("ADANA", dto.getProvince());
        assertEquals("CUKUROVA", dto.getDistrict());
        assertEquals("BOZCALAR", dto.getNeighborhood());
        assertEquals("İlksel", dto.getSolutionQuality());

        assertNotNull(dto.getSourcePayloadHash());
        assertNotNull(dto.getNormalizedHash());
    }

    @Test
    void testMagnitudeHierarchyMwOverMlOverMd() {
        // Case 1: Mw takes precedence
        KoeriRawEventDto rawMw = new KoeriRawEventDto(
                "2026.10.14", "12:00:00", "38.0", "35.0", "10.0",
                "2.8", "3.0", "3.2", "Test Location (KONYA)", "İlksel", "line"
        );
        NormalizedReportDto dtoMw = normalizer.normalize(rawMw);
        assertEquals(3.2, dtoMw.getMagnitudeValue());
        assertEquals("MW", dtoMw.getMagnitudeType());
        assertEquals(2.8, dtoMw.getMagMd());
        assertEquals(3.0, dtoMw.getMagMl());
        assertEquals(3.2, dtoMw.getMagMw());

        // Case 2: ML takes precedence over MD if Mw is null
        KoeriRawEventDto rawMl = new KoeriRawEventDto(
                "2026.10.14", "12:00:00", "38.0", "35.0", "10.0",
                "2.8", "3.0", null, "Test Location (KONYA)", "İlksel", "line"
        );
        NormalizedReportDto dtoMl = normalizer.normalize(rawMl);
        assertEquals(3.0, dtoMl.getMagnitudeValue());
        assertEquals("ML", dtoMl.getMagnitudeType());

        // Case 3: MD is used when Mw and ML are null
        KoeriRawEventDto rawMd = new KoeriRawEventDto(
                "2026.10.14", "12:00:00", "38.0", "35.0", "10.0",
                "2.8", null, null, "Test Location (KONYA)", "İlksel", "line"
        );
        NormalizedReportDto dtoMd = normalizer.normalize(rawMd);
        assertEquals(2.8, dtoMd.getMagnitudeValue());
        assertEquals("MD", dtoMd.getMagnitudeType());
    }

    @Test
    void testRejectsInvalidCoordinatesOrDate() {
        KoeriRawEventDto invalidLat = new KoeriRawEventDto(
                "2026.10.14", "12:00:00", "999.0", "35.0", "10.0",
                null, "2.0", null, "Test", "İlksel", "line"
        );
        NormalizedReportDto dtoLat = normalizer.normalize(invalidLat);
        assertFalse(dtoLat.isValid());

        KoeriRawEventDto invalidDate = new KoeriRawEventDto(
                "invalid-date", "12:00:00", "38.0", "35.0", "10.0",
                null, "2.0", null, "Test", "İlksel", "line"
        );
        NormalizedReportDto dtoDate = normalizer.normalize(invalidDate);
        assertFalse(dtoDate.isValid());
    }
}
