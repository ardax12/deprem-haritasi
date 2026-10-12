package com.ag.deprem.service;

import com.ag.deprem.model.dto.AfadRawEventDto;
import com.ag.deprem.model.dto.NormalizedReportDto;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AfadDataNormalizerTest {

    private AfadDataNormalizer normalizer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        normalizer = new AfadDataNormalizer();
        objectMapper = new ObjectMapper();
    }

    @Test
    void testNormalizeValidSampleEvents() throws Exception {
        try (InputStream is = getClass().getResourceAsStream("/fixtures/afad/afad_sample_events.json")) {
            assertNotNull(is, "Fixture file not found");
            List<AfadRawEventDto> rawEvents = objectMapper.readValue(is, new TypeReference<>() {});
            assertEquals(2, rawEvents.size());

            NormalizedReportDto first = normalizer.normalize(rawEvents.get(0));
            assertTrue(first.isValid());
            assertEquals("AFAD", first.getSource());
            assertEquals("731199", first.getSourceEventId());
            assertEquals(37.7725, first.getLatitude());
            assertEquals(36.15617, first.getLongitude());
            assertEquals(7.0, first.getDepthKm());
            assertEquals(2.1, first.getMagnitudeValue());
            assertEquals("ML", first.getMagnitudeType());
            assertEquals("Saimbeyli (Adana)", first.getLocationText());
            assertEquals(Instant.parse("2026-10-12T00:18:23Z"), first.getOccurredAt());
            assertNotNull(first.getNormalizedHash());
            assertNotNull(first.getSourcePayloadHash());
        }
    }

    @Test
    void testNormalizeMissingMagnitudeAndDepth() throws Exception {
        try (InputStream is = getClass().getResourceAsStream("/fixtures/afad/afad_missing_magnitude_event.json")) {
            assertNotNull(is);
            List<AfadRawEventDto> rawEvents = objectMapper.readValue(is, new TypeReference<>() {});
            NormalizedReportDto dto = normalizer.normalize(rawEvents.get(0));

            assertTrue(dto.isValid(), "Record with valid coords and date must be valid even without magnitude");
            assertNull(dto.getMagnitudeValue());
            assertNull(dto.getDepthKm());
            assertTrue(dto.getDataQualityNotes().stream().anyMatch(n -> n.contains("magnitude")));
        }
    }

    @Test
    void testNormalizeInvalidCoordinatesAndTimestamp() throws Exception {
        try (InputStream is = getClass().getResourceAsStream("/fixtures/afad/afad_invalid_events.json")) {
            assertNotNull(is);
            List<AfadRawEventDto> rawEvents = objectMapper.readValue(is, new TypeReference<>() {});

            // First: latitude = 120.5 (> 90)
            NormalizedReportDto invCoord = normalizer.normalize(rawEvents.get(0));
            assertFalse(invCoord.isValid());
            assertTrue(invCoord.getDataQualityNotes().stream().anyMatch(n -> n.contains("latitude")));

            // Second: date = "gecersiz-tarih"
            NormalizedReportDto invDate = normalizer.normalize(rawEvents.get(1));
            assertFalse(invDate.isValid());
            assertTrue(invDate.getDataQualityNotes().stream().anyMatch(n -> n.contains("occurred date")));
        }
    }

    @Test
    void testRevisionCreatesDifferentNormalizedHash() throws Exception {
        try (InputStream isSample = getClass().getResourceAsStream("/fixtures/afad/afad_sample_events.json");
             InputStream isUpdated = getClass().getResourceAsStream("/fixtures/afad/afad_updated_event.json")) {

            List<AfadRawEventDto> samples = objectMapper.readValue(isSample, new TypeReference<>() {});
            List<AfadRawEventDto> updated = objectMapper.readValue(isUpdated, new TypeReference<>() {});

            NormalizedReportDto original = normalizer.normalize(samples.get(0));
            NormalizedReportDto revised = normalizer.normalize(updated.get(0));

            assertEquals(original.getSourceEventId(), revised.getSourceEventId());
            assertNotEquals(original.getMagnitudeValue(), revised.getMagnitudeValue());
            assertNotEquals(original.getNormalizedHash(), revised.getNormalizedHash());
        }
    }
}
