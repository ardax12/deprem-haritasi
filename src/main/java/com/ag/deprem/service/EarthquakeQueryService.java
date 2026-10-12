package com.ag.deprem.service;

import com.ag.deprem.model.dto.*;
import com.ag.deprem.model.entity.SourceReport;
import com.ag.deprem.model.entity.SourceReportRevision;
import com.ag.deprem.repository.SourceReportRepository;
import com.ag.deprem.repository.SourceReportRevisionRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class EarthquakeQueryService {

    private final SourceReportRevisionRepository revisionRepository;
    private final SourceReportRepository reportRepository;
    private final SourceStatusTracker statusTracker;
    private final LocationParser locationParser;

    public EarthquakeQueryService(SourceReportRevisionRepository revisionRepository,
                                  SourceReportRepository reportRepository,
                                  SourceStatusTracker statusTracker,
                                  LocationParser locationParser) {
        this.revisionRepository = revisionRepository;
        this.reportRepository = reportRepository;
        this.statusTracker = statusTracker;
        this.locationParser = locationParser;
    }

    public EarthquakesListResponseDto queryEarthquakes(Instant start,
                                                       Instant end,
                                                       Double minMagnitude,
                                                       Double maxMagnitude,
                                                       String magnitudeType,
                                                       Double minDepthKm,
                                                       Double maxDepthKm,
                                                       String bbox,
                                                       String place,
                                                       int page,
                                                       int pageSize,
                                                       String sortParam) {
        Instant now = Instant.now();
        Instant effectiveEnd = (end != null) ? end : now;
        Instant effectiveStart = (start != null) ? start : effectiveEnd.minus(24, ChronoUnit.HOURS);

        if (effectiveStart.isAfter(effectiveEnd)) {
            throw new IllegalArgumentException("Parameter 'start' must be before 'end'");
        }

        int effectivePageSize = Math.clamp(pageSize, 1, 500);
        int effectivePage = Math.max(page, 0);

        Sort sort = parseSort(sortParam);
        Pageable pageable = PageRequest.of(effectivePage, effectivePageSize, sort);

        Double[] bboxCoords = parseBbox(bbox);

        Specification<SourceReportRevision> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Match only the active current revision of each source report
            predicates.add(cb.equal(root.get("id"), root.get("sourceReport").get("currentRevisionId")));

            // Time range: occurredAt >= start and occurredAt < end
            predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), effectiveStart));
            predicates.add(cb.lessThan(root.get("occurredAt"), effectiveEnd));

            // Magnitude filters
            if (minMagnitude != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("magnitudeValue"), minMagnitude));
            }
            if (maxMagnitude != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("magnitudeValue"), maxMagnitude));
            }
            if (magnitudeType != null && !magnitudeType.isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("magnitudeType")), magnitudeType.trim().toUpperCase()));
            }

            // Depth filters
            if (minDepthKm != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("depthKm"), minDepthKm));
            }
            if (maxDepthKm != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("depthKm"), maxDepthKm));
            }

            // Bounding box filter: west, south, east, north
            if (bboxCoords != null) {
                double west = bboxCoords[0];
                double south = bboxCoords[1];
                double east = bboxCoords[2];
                double north = bboxCoords[3];

                predicates.add(cb.greaterThanOrEqualTo(root.get("latitude"), south));
                predicates.add(cb.lessThanOrEqualTo(root.get("latitude"), north));

                if (west <= east) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("longitude"), west));
                    predicates.add(cb.lessThanOrEqualTo(root.get("longitude"), east));
                } else {
                    // Antimeridian crossing
                    Predicate westPart = cb.greaterThanOrEqualTo(root.get("longitude"), west);
                    Predicate eastPart = cb.lessThanOrEqualTo(root.get("longitude"), east);
                    predicates.add(cb.or(westPart, eastPart));
                }
            }

            // Place search
            if (place != null && !place.isBlank()) {
                String pattern = "%" + place.trim().toLowerCase() + "%";
                Predicate inLocation = cb.like(cb.lower(root.get("locationText")), pattern);
                Predicate inProvince = cb.like(cb.lower(root.get("province")), pattern);
                Predicate inDistrict = cb.like(cb.lower(root.get("district")), pattern);
                predicates.add(cb.or(inLocation, inProvince, inDistrict));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<SourceReportRevision> revisionPage = revisionRepository.findAll(spec, pageable);

        List<EarthquakeItemDto> items = revisionPage.getContent().stream()
                .map(this::mapToItemDto)
                .toList();

        String nextCursor = revisionPage.hasNext() ? String.valueOf(effectivePage + 1) : null;
        PageInfoDto pageInfo = new PageInfoDto(
                revisionPage.getSize(),
                revisionPage.getTotalElements(),
                revisionPage.getTotalPages(),
                revisionPage.getNumber(),
                nextCursor
        );

        FreshnessDto freshness = new FreshnessDto(
                "AFAD",
                statusTracker.getLastSuccessfulFetchAt(),
                statusTracker.getFreshnessStatus(now)
        );

        return new EarthquakesListResponseDto(items, pageInfo, freshness);
    }

    public Optional<EarthquakeDetailDto> getEarthquakeDetail(String idStr) {
        Optional<SourceReport> reportOpt = findReportByIdString(idStr);
        if (reportOpt.isEmpty()) {
            return Optional.empty();
        }

        SourceReport report = reportOpt.get();
        List<SourceReportRevision> revisions = revisionRepository
                .findBySourceReportIdOrderByRevisionNumberDesc(report.getId());

        if (revisions.isEmpty()) {
            return Optional.empty();
        }

        EarthquakeItemDto currentDto = mapToItemDto(revisions.get(0));
        List<EarthquakeItemDto> history = revisions.stream()
                .map(this::mapToItemDto)
                .toList();

        return Optional.of(new EarthquakeDetailDto(currentDto, history));
    }

    public StatusResponseDto getStatus() {
        Instant now = Instant.now();
        Instant lastFetch = statusTracker.getLastSuccessfulFetchAt();
        Instant lastAttempt = statusTracker.getLastAttemptAt();
        String freshness = statusTracker.getFreshnessStatus(now);

        Instant coverageFrom = revisionRepository.findLatestOccurredAt().orElse(null);

        StatusResponseDto.CoverageDto coverage = new StatusResponseDto.CoverageDto(
                coverageFrom,
                lastFetch != null ? lastFetch : now,
                statusTracker.isCoverageComplete()
        );

        StatusResponseDto.SourceStatusDto sourceStatus = new StatusResponseDto.SourceStatusDto(
                "AFAD",
                freshness,
                lastFetch,
                lastAttempt,
                coverage
        );

        return new StatusResponseDto("available", "live", List.of(sourceStatus));
    }

    private Optional<SourceReport> findReportByIdString(String idStr) {
        if (idStr == null || idStr.isBlank()) {
            return Optional.empty();
        }
        if (idStr.toLowerCase().startsWith("afad:")) {
            String eventId = idStr.substring(5);
            return reportRepository.findBySourceAndSourceEventId("AFAD", eventId);
        }
        try {
            UUID uuid = UUID.fromString(idStr);
            return reportRepository.findById(uuid);
        } catch (IllegalArgumentException e) {
            return reportRepository.findBySourceAndSourceEventId("AFAD", idStr);
        }
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

        LocationInfoDto locInfo = locationParser.parse(
                rev.getLocationText(),
                rev.getProvince(),
                rev.getDistrict(),
                rev.getCountry()
        );
        dto.setIsOffshore(locInfo.getIsOffshore());
        dto.setDistanceToCoastKm(locInfo.getDistanceToCoastKm());
        dto.setNearestSettlement(locInfo.getNearestSettlement());
        dto.setEnvironment(locInfo.getEnvironment());
        dto.setDisplayLocation(locInfo.getDisplayLocation());

        dto.setSource(report.getSource());
        dto.setRevisionNumber(rev.getRevisionNumber());
        dto.setRevisionStatus(rev.getId().equals(report.getCurrentRevisionId()) ? "current" : "superseded");

        if (rev.getMagnitudeValue() == null) {
            dto.getDataQuality().add("Büyüklük raporlanmadı");
        }
        if (rev.getDepthKm() == null) {
            dto.getDataQuality().add("Derinlik raporlanmadı");
        }

        return dto;
    }

    private Sort parseSort(String sortParam) {
        if (sortParam == null || sortParam.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "occurredAt");
        }
        String[] parts = sortParam.split(":");
        String field = parts[0].trim();
        Sort.Direction direction = (parts.length > 1 && parts[1].trim().equalsIgnoreCase("asc"))
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        if ("magnitude".equalsIgnoreCase(field)) {
            return Sort.by(direction, "magnitudeValue").and(Sort.by(Sort.Direction.DESC, "occurredAt"));
        } else if ("depth".equalsIgnoreCase(field)) {
            return Sort.by(direction, "depthKm");
        } else {
            return Sort.by(direction, "occurredAt");
        }
    }

    private Double[] parseBbox(String bbox) {
        if (bbox == null || bbox.isBlank()) {
            return null;
        }
        String[] parts = bbox.split(",");
        if (parts.length != 4) {
            throw new IllegalArgumentException("bbox must contain 4 comma-separated values: west,south,east,north");
        }
        try {
            double west = Double.parseDouble(parts[0].trim());
            double south = Double.parseDouble(parts[1].trim());
            double east = Double.parseDouble(parts[2].trim());
            double north = Double.parseDouble(parts[3].trim());

            if (south < -90 || south > 90 || north < -90 || north > 90 || south > north) {
                throw new IllegalArgumentException("Invalid latitude bounds in bbox: south=" + south + ", north=" + north);
            }
            if (west < -180 || west > 180 || east < -180 || east > 180) {
                throw new IllegalArgumentException("Invalid longitude bounds in bbox: west=" + west + ", east=" + east);
            }
            return new Double[]{west, south, east, north};
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Malformed numeric value in bbox: " + bbox);
        }
    }
}
