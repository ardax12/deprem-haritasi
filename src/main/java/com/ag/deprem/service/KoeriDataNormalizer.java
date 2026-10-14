package com.ag.deprem.service;

import com.ag.deprem.model.dto.KoeriRawEventDto;
import com.ag.deprem.model.dto.NormalizedReportDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class KoeriDataNormalizer {

    private static final Logger log = LoggerFactory.getLogger(KoeriDataNormalizer.class);

    private static final DateTimeFormatter KOERI_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss");
    private static final DateTimeFormatter UTC_COMPOSITE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneOffset.UTC);
    private static final ZoneId TURKEY_ZONE = ZoneId.of("Europe/Istanbul");

    private static final Pattern PAREN_PROVINCE_PATTERN = Pattern.compile("^(.*?)\\s*\\(([^\\)]+)\\)\\s*$");

    public NormalizedReportDto normalize(KoeriRawEventDto raw) {
        if (raw == null) {
            return null;
        }

        NormalizedReportDto dto = new NormalizedReportDto();
        dto.setSource("KOERI");
        dto.setValid(true);

        // 1. Validate and parse timestamp (KOERI lst0.asp is in Turkey local time Europe/Istanbul, UTC+3)
        Instant occurredAt = parseTurkeyTimestamp(raw.getDate(), raw.getTime());
        if (occurredAt == null) {
            dto.setValid(false);
            dto.getDataQualityNotes().add("Invalid date/time: " + raw.getDate() + " " + raw.getTime());
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

        // 3. Generate deterministic composite sourceEventId: koeri-yyyyMMddHHmmss-lat-lon (<= 64 chars)
        String utcTimeStr = UTC_COMPOSITE_FORMATTER.format(occurredAt);
        String eventId = String.format(Locale.ROOT, "koeri-%s-%.4f-%.4f", utcTimeStr, latitude, longitude);
        dto.setSourceEventId(eventId);

        // 4. Depth
        Double depth = parseDouble(raw.getDepth());
        if (depth == null) {
            dto.getDataQualityNotes().add("Missing or unparseable depth");
        }
        dto.setDepthKm(depth);

        // 5. Magnitudes (MD, ML, MW)
        Double md = parseDouble(raw.getMd());
        Double ml = parseDouble(raw.getMl());
        Double mw = parseDouble(raw.getMw());

        dto.setMagMd(md);
        dto.setMagMl(ml);
        dto.setMagMw(mw);

        // Canonical magnitude selection hierarchy: Mw > ML > MD
        if (mw != null) {
            dto.setMagnitudeValue(mw);
            dto.setMagnitudeType("MW");
        } else if (ml != null) {
            dto.setMagnitudeValue(ml);
            dto.setMagnitudeType("ML");
        } else if (md != null) {
            dto.setMagnitudeValue(md);
            dto.setMagnitudeType("MD");
        } else {
            dto.setMagnitudeValue(null);
            dto.setMagnitudeType(null);
            dto.getDataQualityNotes().add("Missing magnitude values (MD, ML, MW all absent or -.-)");
        }

        // 6. Location and administrative divisions
        String cleanLocation = cleanText(raw.getLocation());
        dto.setLocationText(cleanLocation);
        parseLocationDetails(cleanLocation, dto);

        // 7. Solution Quality
        String quality = cleanText(raw.getSolutionQuality());
        dto.setSolutionQuality(quality);

        dto.setIsEventUpdate(false);

        // 8. Payload and Normalized Hashes
        dto.setSourcePayloadHash(computeRawHash(raw));
        dto.setNormalizedHash(computeNormalizedHash(dto));

        return dto;
    }

    private Instant parseTurkeyTimestamp(String dateStr, String timeStr) {
        if (dateStr == null || timeStr == null || dateStr.isBlank() || timeStr.isBlank()) {
            return null;
        }
        try {
            String combined = dateStr.trim() + " " + timeStr.trim();
            LocalDateTime ldt = LocalDateTime.parse(combined, KOERI_DATE_TIME_FORMATTER);
            return ldt.atZone(TURKEY_ZONE).toInstant();
        } catch (DateTimeParseException e) {
            log.debug("Failed to parse KOERI timestamp '{}/{}': {}", dateStr, timeStr, e.getMessage());
            return null;
        }
    }

    private void parseLocationDetails(String loc, NormalizedReportDto dto) {
        if (loc == null || loc.isBlank()) {
            dto.setCountry("Türkiye");
            return;
        }

        Matcher matcher = PAREN_PROVINCE_PATTERN.matcher(loc);
        if (matcher.matches()) {
            String leftPart = matcher.group(1).trim();
            String provincePart = matcher.group(2).trim();

            dto.setProvince(provincePart);

            // Check if leftPart contains hyphen like "BOZCALAR-CUKUROVA"
            if (leftPart.contains("-")) {
                String[] segments = leftPart.split("-");
                if (segments.length >= 2) {
                    dto.setNeighborhood(segments[0].trim());
                    dto.setDistrict(segments[segments.length - 1].trim());
                } else {
                    dto.setDistrict(leftPart);
                }
            } else {
                dto.setDistrict(leftPart);
            }
        } else {
            dto.setDistrict(loc);
        }

        dto.setCountry("Türkiye");
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
        if (trimmed.isEmpty() || trimmed.equals("-.-") || trimmed.equals("-")) {
            return null;
        }
        return trimmed.replaceAll("<[^>]*>", "").replaceAll("&[a-zA-Z]+;", " ");
    }

    private String computeRawHash(KoeriRawEventDto raw) {
        String data = String.format("%s|%s|%s|%s|%s|%s|%s|%s|%s|%s",
                raw.getDate(), raw.getTime(), raw.getLatitude(), raw.getLongitude(),
                raw.getDepth(), raw.getMd(), raw.getMl(), raw.getMw(),
                raw.getLocation(), raw.getSolutionQuality());
        return sha256Hex(data);
    }

    private String computeNormalizedHash(NormalizedReportDto dto) {
        String data = String.format("%s|%s|%s|%s|%s|%s|%s|%s|%s",
                dto.getSourceEventId(),
                dto.getOccurredAt(),
                dto.getLatitude(),
                dto.getLongitude(),
                dto.getDepthKm(),
                dto.getMagnitudeValue(),
                dto.getMagnitudeType(),
                dto.getLocationText(),
                dto.getSolutionQuality());
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
