package com.ag.deprem.service;

import com.ag.deprem.model.dto.AfadRawEventDto;
import com.ag.deprem.model.dto.NormalizedReportDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HexFormat;

@Service
public class AfadDataNormalizer {

    private static final Logger log = LoggerFactory.getLogger(AfadDataNormalizer.class);

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter SPACE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public NormalizedReportDto normalize(AfadRawEventDto raw) {
        if (raw == null) {
            return null;
        }

        NormalizedReportDto dto = new NormalizedReportDto();
        dto.setSource("AFAD");
        dto.setSourceEventId(cleanText(raw.getEventId()));

        if (dto.getSourceEventId() == null || dto.getSourceEventId().isBlank()) {
            dto.setValid(false);
            dto.getDataQualityNotes().add("Missing event ID");
            return dto;
        }

        // 1. Validate and parse occurred date (interpreted as UTC)
        Instant occurredAt = parseUtcTimestamp(raw.getDate());
        if (occurredAt == null) {
            dto.setValid(false);
            dto.getDataQualityNotes().add("Invalid or missing occurred date: " + raw.getDate());
            return dto;
        }
        dto.setOccurredAt(occurredAt);

        // 2. Validate coordinates
        Double latitude = parseDouble(raw.getLatitude());
        Double longitude = parseDouble(raw.getLongitude());

        if (latitude == null || latitude < -90.0 || latitude > 90.0) {
            dto.setValid(false);
            dto.getDataQualityNotes().add("Invalid latitude: " + raw.getLatitude());
            return dto;
        }

        if (longitude == null || longitude < -180.0 || longitude > 180.0) {
            dto.setValid(false);
            dto.getDataQualityNotes().add("Invalid longitude: " + raw.getLongitude());
            return dto;
        }

        dto.setLatitude(latitude);
        dto.setLongitude(longitude);

        // 3. Optional fields: magnitude, depth, location
        Double magnitude = parseDouble(raw.getMagnitude());
        if (magnitude == null) {
            dto.getDataQualityNotes().add("Missing or unparseable magnitude");
        }
        dto.setMagnitudeValue(magnitude);
        String magType = cleanText(raw.getType());
        dto.setMagnitudeType(magType);
        if (magType != null && magnitude != null) {
            if ("MW".equalsIgnoreCase(magType)) {
                dto.setMagMw(magnitude);
            } else if ("ML".equalsIgnoreCase(magType)) {
                dto.setMagMl(magnitude);
            } else if ("MD".equalsIgnoreCase(magType)) {
                dto.setMagMd(magnitude);
            }
        }

        Double depth = parseDouble(raw.getDepth());
        if (depth == null) {
            dto.getDataQualityNotes().add("Missing or unparseable depth");
        }
        dto.setDepthKm(depth);

        dto.setLocationText(cleanText(raw.getLocation()));
        dto.setCountry(cleanText(raw.getCountry()));
        dto.setProvince(cleanText(raw.getProvince()));
        dto.setDistrict(cleanText(raw.getDistrict()));
        dto.setNeighborhood(cleanText(raw.getNeighborhood()));

        dto.setIsEventUpdate(Boolean.TRUE.equals(raw.getIsEventUpdate()));
        if (raw.getLastUpdateDate() != null) {
            dto.setSourceUpdatedAt(parseUtcTimestamp(raw.getLastUpdateDate()));
        }

        // 4. Hashes
        dto.setSourcePayloadHash(computeRawHash(raw));
        dto.setNormalizedHash(computeNormalizedHash(dto));

        dto.setValid(true);
        return dto;
    }

    private Instant parseUtcTimestamp(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        String trimmed = dateStr.trim();
        try {
            if (trimmed.contains("T")) {
                return LocalDateTime.parse(trimmed, ISO_FORMATTER).toInstant(ZoneOffset.UTC);
            } else {
                return LocalDateTime.parse(trimmed, SPACE_FORMATTER).toInstant(ZoneOffset.UTC);
            }
        } catch (DateTimeParseException e) {
            log.debug("Failed to parse date '{}': {}", dateStr, e.getMessage());
            return null;
        }
    }

    private Double parseDouble(String str) {
        if (str == null || str.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(str.trim().replace(",", "."));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String cleanText(String str) {
        if (str == null) {
            return null;
        }
        String trimmed = str.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        // Basic HTML entity and tag stripping for security
        return trimmed.replaceAll("<[^>]*>", "").replaceAll("&[a-zA-Z]+;", " ");
    }

    private String computeRawHash(AfadRawEventDto raw) {
        String data = String.format("%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s",
                raw.getEventId(), raw.getDate(), raw.getLatitude(), raw.getLongitude(),
                raw.getMagnitude(), raw.getType(), raw.getDepth(), raw.getLocation(),
                raw.getIsEventUpdate(), raw.getLastUpdateDate(), raw.getRms());
        return sha256Hex(data);
    }

    private String computeNormalizedHash(NormalizedReportDto dto) {
        String data = String.format("%s|%s|%s|%s|%s|%s|%s|%s",
                dto.getSourceEventId(),
                dto.getOccurredAt(),
                dto.getLatitude(),
                dto.getLongitude(),
                dto.getDepthKm(),
                dto.getMagnitudeValue(),
                dto.getMagnitudeType(),
                dto.getLocationText());
        return sha256Hex(data);
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
