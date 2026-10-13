package com.ag.deprem.service;

import com.ag.deprem.model.dto.ArchiveManifestDto;
import com.ag.deprem.model.dto.ArchiveShardDto;
import com.ag.deprem.model.dto.EarthquakeItemDto;
import com.ag.deprem.model.dto.MagnitudeDto;
import com.ag.deprem.model.entity.ArchiveManifest;
import com.ag.deprem.model.entity.SourceReport;
import com.ag.deprem.model.entity.SourceReportRevision;
import com.ag.deprem.repository.ArchiveManifestRepository;
import com.ag.deprem.repository.SourceReportRevisionRepository;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;

@Service
public class ArchiveExporterService {

    private static final Logger log = LoggerFactory.getLogger(ArchiveExporterService.class);
    private static final String SCHEMA_VERSION = "1.0";
    private static final String SOURCE_COVERAGE = "AFAD";

    private final SourceReportRevisionRepository revisionRepository;
    private final ArchiveManifestRepository manifestRepository;
    private final ObjectMapper objectMapper;
    private final String archiveDirectory;

    public ArchiveExporterService(SourceReportRevisionRepository revisionRepository,
                                  ArchiveManifestRepository manifestRepository,
                                  ObjectMapper objectMapper,
                                  @Value("${app.archive.directory:./archive}") String archiveDirectory) {
        this.revisionRepository = revisionRepository;
        this.manifestRepository = manifestRepository;
        this.objectMapper = objectMapper;
        this.archiveDirectory = archiveDirectory;
    }

    private static final java.util.regex.Pattern SAFE_VERSION_PATTERN = java.util.regex.Pattern.compile("^[a-zA-Z0-9_-]{1,64}$");

    /**
     * Exports an archive shard for the specified time interval, calculates SHA-256 checksum,
     * persists manifest metadata to the database, and writes shard JSON to disk.
     */
    @Transactional
    public ArchiveManifestDto exportArchive(Instant start, Instant end, String archiveVersion) {
        if (start == null || end == null || !start.isBefore(end)) {
            throw new IllegalArgumentException("Parameter 'start' must be strictly before 'end'");
        }
        validateArchiveVersion(archiveVersion);

        log.info("Exporting archive shard '{}' for interval [{} to {}]", archiveVersion, start, end);

        List<SourceReportRevision> revisions = revisionRepository.findAllCurrentRevisionsBetween(start, end);
        List<EarthquakeItemDto> items = revisions.stream()
                .map(this::mapToItemDto)
                .toList();

        Instant now = Instant.now();
        ArchiveShardDto shardDto = new ArchiveShardDto(
                SCHEMA_VERSION,
                archiveVersion,
                now,
                start,
                end,
                SOURCE_COVERAGE,
                items.size(),
                items
        );

        try {
            // Ensure archive directory exists
            Path dirPath = Paths.get(archiveDirectory).toAbsolutePath().normalize();
            Files.createDirectories(dirPath);

            // Path traversal protection
            Path shardFilePath = dirPath.resolve(archiveVersion + ".json").normalize();
            if (!shardFilePath.startsWith(dirPath)) {
                throw new SecurityException("Archive path escapes destination directory: " + archiveVersion);
            }

            // Serialize to JSON bytes
            byte[] jsonBytes = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(shardDto);

            // Compute SHA-256 checksum
            String checksum = computeSha256(jsonBytes);

            // Write shard file to disk
            Files.write(shardFilePath, jsonBytes);
            log.info("Written archive shard file: {} (size: {} bytes, records: {})",
                    shardFilePath.toAbsolutePath(), jsonBytes.length, items.size());

            // Save or update ArchiveManifest entity in database
            ArchiveManifest manifest = manifestRepository.findByArchiveVersion(archiveVersion)
                    .orElseGet(ArchiveManifest::new);

            if (manifest.getId() == null) {
                manifest.setId(UUID.randomUUID());
            }
            manifest.setArchiveVersion(archiveVersion);
            manifest.setStartAt(start);
            manifest.setEndAt(end);
            manifest.setRecordCount((long) items.size());
            manifest.setGeneratedAt(now);
            manifest.setChecksum(checksum);
            manifest.setSchemaVersion(SCHEMA_VERSION);
            manifest.setSourceCoverage(SOURCE_COVERAGE);
            manifest.setVerified(false);

            manifest = manifestRepository.save(manifest);

            // Explicitly verify the written file on disk before marking verified
            boolean isVerified = verifyArchive(archiveVersion);
            if (!isVerified) {
                throw new IllegalStateException("Immediate archive verification failed for version: " + archiveVersion);
            }
            manifest.setVerified(true);
            manifest = manifestRepository.save(manifest);

            // Update master manifest.json index file in archive directory
            updateMasterManifestFile(dirPath);

            log.info("Archive shard '{}' exported and verified with checksum: {}", archiveVersion, checksum);

            return mapToManifestDto(manifest);
        } catch (IOException | NoSuchAlgorithmException e) {
            log.error("Failed to export archive shard '{}': {}", archiveVersion, e.getMessage(), e);
            throw new RuntimeException("Arşiv dışa aktarma başarısız: " + e.getMessage(), e);
        }
    }

    public static void validateArchiveVersion(String archiveVersion) {
        if (archiveVersion == null || archiveVersion.isBlank()) {
            throw new IllegalArgumentException("Parameter 'archiveVersion' cannot be blank");
        }
        if (!SAFE_VERSION_PATTERN.matcher(archiveVersion.trim()).matches()) {
            throw new IllegalArgumentException("Invalid archiveVersion format. Allowed characters: alphanumeric, underscore, hyphen (1-64 chars)");
        }
    }

    /**
     * Verifies the file integrity of an existing archive on disk against DB checksum.
     */
    @Transactional
    public boolean verifyArchive(String archiveVersion) {
        Optional<ArchiveManifest> manifestOpt = manifestRepository.findByArchiveVersion(archiveVersion);
        if (manifestOpt.isEmpty()) {
            log.warn("Cannot verify archive '{}': manifest record not found in database", archiveVersion);
            return false;
        }

        ArchiveManifest manifest = manifestOpt.get();
        Path shardFilePath = Paths.get(archiveDirectory).resolve(archiveVersion + ".json");
        if (!Files.exists(shardFilePath)) {
            log.warn("Cannot verify archive '{}': file {} does not exist on disk", archiveVersion, shardFilePath);
            manifest.setVerified(false);
            manifestRepository.save(manifest);
            return false;
        }

        try {
            byte[] fileBytes = Files.readAllBytes(shardFilePath);
            String calculatedChecksum = computeSha256(fileBytes);

            boolean matches = calculatedChecksum.equalsIgnoreCase(manifest.getChecksum());
            if (matches) {
                // Verify record count by reading back
                ArchiveShardDto shard = objectMapper.readValue(fileBytes, ArchiveShardDto.class);
                if (shard.getRecordCount() == manifest.getRecordCount().intValue()) {
                    manifest.setVerified(true);
                    manifestRepository.save(manifest);
                    log.info("Archive '{}' successfully verified against checksum and record count", archiveVersion);
                    return true;
                }
            }

            log.error("Archive '{}' verification failed: checksum mismatch (expected {}, calculated {})",
                    archiveVersion, manifest.getChecksum(), calculatedChecksum);
            manifest.setVerified(false);
            manifestRepository.save(manifest);
            return false;
        } catch (Exception e) {
            log.error("Error verifying archive '{}': {}", archiveVersion, e.getMessage(), e);
            manifest.setVerified(false);
            manifestRepository.save(manifest);
            return false;
        }
    }

    @Transactional(readOnly = true)
    public List<ArchiveManifestDto> listManifests() {
        return manifestRepository.findAll(Sort.by(Sort.Direction.DESC, "startAt")).stream()
                .map(this::mapToManifestDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<ArchiveManifestDto> getManifest(String archiveVersion) {
        return manifestRepository.findByArchiveVersion(archiveVersion)
                .map(this::mapToManifestDto);
    }

    private void updateMasterManifestFile(Path dirPath) {
        try {
            List<ArchiveManifestDto> allManifests = listManifests();
            Path masterManifestPath = dirPath.resolve("manifest.json");
            byte[] bytes = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(allManifests);
            Files.write(masterManifestPath, bytes);
        } catch (Exception e) {
            log.warn("Could not write master manifest.json: {}", e.getMessage());
        }
    }

    private String computeSha256(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data);
        return HexFormat.of().formatHex(hash);
    }

    private ArchiveManifestDto mapToManifestDto(ArchiveManifest m) {
        String downloadUrl = "/archive/" + m.getArchiveVersion() + ".json";
        return new ArchiveManifestDto(
                m.getId(),
                m.getArchiveVersion(),
                m.getStartAt(),
                m.getEndAt(),
                m.getRecordCount(),
                m.getGeneratedAt(),
                m.getChecksum(),
                m.getSchemaVersion(),
                m.getSourceCoverage(),
                m.getVerified(),
                downloadUrl
        );
    }

    private EarthquakeItemDto mapToItemDto(SourceReportRevision rev) {
        EarthquakeItemDto dto = new EarthquakeItemDto();
        SourceReport report = rev.getSourceReport();

        dto.setId(report.getSource().toLowerCase() + ":" + report.getSourceEventId());
        dto.setOccurredAt(rev.getOccurredAt());
        dto.setRetrievedAt(rev.getObservedAt());
        dto.setLatitude(rev.getLatitude());
        dto.setLongitude(rev.getLongitude());
        dto.setDepthKm(rev.getDepthKm());

        if (rev.getMagnitudeValue() != null || rev.getMagnitudeType() != null) {
            dto.setMagnitude(new MagnitudeDto(rev.getMagnitudeValue(), rev.getMagnitudeType()));
        }

        dto.setPlace(rev.getLocationText());
        dto.setCountry(rev.getCountry());
        dto.setProvince(rev.getProvince());
        dto.setDistrict(rev.getDistrict());
        dto.setNeighborhood(rev.getNeighborhood());
        dto.setSource(report.getSource());
        dto.setRevisionNumber(rev.getRevisionNumber());
        return dto;
    }
}
