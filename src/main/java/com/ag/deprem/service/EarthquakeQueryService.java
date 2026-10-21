package com.ag.deprem.service;

import com.ag.deprem.model.dto.*;
import com.ag.deprem.model.entity.EventMatch;
import com.ag.deprem.model.entity.SourceReport;
import com.ag.deprem.model.entity.SourceReportRevision;
import com.ag.deprem.repository.EventMatchRepository;
import com.ag.deprem.repository.SourceReportRepository;
import com.ag.deprem.repository.SourceReportRevisionRepository;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.CriteriaBuilder;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class EarthquakeQueryService {

    private final SourceReportRevisionRepository revisionRepository;
    private final SourceReportRepository reportRepository;
    private final EventMatchRepository matchRepository;
    private final SourceStatusTracker statusTracker;
    private final LocationParser locationParser;

    public EarthquakeQueryService(SourceReportRevisionRepository revisionRepository,
                                  SourceReportRepository reportRepository,
                                  EventMatchRepository matchRepository,
                                  SourceStatusTracker statusTracker,
                                  LocationParser locationParser) {
        this.revisionRepository = revisionRepository;
        this.reportRepository = reportRepository;
        this.matchRepository = matchRepository;
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
                                                       String source,
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

            // Source filter
            if (source != null && !source.isBlank() && !"ALL".equalsIgnoreCase(source.trim())) {
                predicates.add(cb.equal(cb.upper(root.get("sourceReport").get("source")), source.trim().toUpperCase()));
            }

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

            // Place search with Turkish I/İ case folding
            if (place != null && !place.isBlank()) {
                String pattern = "%" + turkishFold(place.trim()) + "%";
                Predicate inLocation = cb.like(turkishFold(cb, root.get("locationText")), pattern);
                Predicate inProvince = cb.like(turkishFold(cb, root.get("province")), pattern);
                Predicate inDistrict = cb.like(turkishFold(cb, root.get("district")), pattern);
                predicates.add(cb.or(inLocation, inProvince, inDistrict));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<SourceReportRevision> revisionPage = revisionRepository.findAll(spec, pageable);

        List<UUID> reportIds = revisionPage.getContent().stream()
                .map(r -> r.getSourceReport().getId())
                .toList();
        List<EventMatch> matches = reportIds.isEmpty() ? List.of() : matchRepository.findActiveMatchesByReportIds(reportIds);
        Map<UUID, EventMatch> matchByReportId = new HashMap<>();
        for (EventMatch m : matches) {
            if (m.getLeftReport() != null) {
                matchByReportId.putIfAbsent(m.getLeftReport().getId(), m);
            }
            if (m.getRightReport() != null) {
                matchByReportId.putIfAbsent(m.getRightReport().getId(), m);
            }
        }

        List<EarthquakeItemDto> items = revisionPage.getContent().stream()
                .map(rev -> {
                    EarthquakeItemDto dto = mapToItemDto(rev);
                    UUID reportId = rev.getSourceReport().getId();
                    EventMatch match = matchByReportId.get(reportId);
                    if (match != null) {
                        dto.setMatchStatus(match.getStatus());
                        SourceReport partner = match.getLeftReport().getId().equals(reportId)
                                ? match.getRightReport()
                                : match.getLeftReport();
                        if (partner != null) {
                            dto.setMatchedEventId(partner.getSource().toLowerCase() + ":" + partner.getSourceEventId());
                        }
                    }
                    return dto;
                })
                .toList();

        String nextCursor = revisionPage.hasNext() ? String.valueOf(effectivePage + 1) : null;
        PageInfoDto pageInfo = new PageInfoDto(
                revisionPage.getSize(),
                revisionPage.getTotalElements(),
                revisionPage.getTotalPages(),
                revisionPage.getNumber(),
                nextCursor
        );

        String freshnessSource = (source != null && "KOERI".equalsIgnoreCase(source.trim())) ? "KOERI" : "AFAD";
        FreshnessDto freshness = new FreshnessDto(
                freshnessSource,
                statusTracker.getLastSuccessfulFetchAt(freshnessSource),
                statusTracker.getFreshnessStatus(freshnessSource, now)
        );

        return new EarthquakesListResponseDto(items, pageInfo, freshness);
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
        return queryEarthquakes(start, end, minMagnitude, maxMagnitude, magnitudeType,
                minDepthKm, maxDepthKm, bbox, place, null, page, pageSize, sortParam);
    }

    public Optional<EarthquakeDetailDto> getEarthquakeDetail(String idStr) {
        Optional<SourceReport> reportOpt = reportRepository.findReportByIdString(idStr);
        if (reportOpt.isEmpty()) {
            return Optional.empty();
        }

        SourceReport report = reportOpt.get();
        List<SourceReportRevision> revisions = revisionRepository
                .findBySourceReportIdOrderByRevisionNumberDesc(report.getId());

        if (revisions.isEmpty()) {
            return Optional.empty();
        }

        SourceReportRevision currentRev = revisions.stream()
                .filter(r -> r.getId().equals(report.getCurrentRevisionId()))
                .findFirst()
                .orElse(revisions.get(0));
        EarthquakeItemDto currentDto = mapToItemDto(currentRev);
        List<EarthquakeItemDto> history = revisions.stream()
                .map(this::mapToItemDto)
                .toList();

        EarthquakeDetailDto detail = new EarthquakeDetailDto(currentDto, history);

        // Find active matches
        List<EventMatch> matches = matchRepository.findAllByReportId(report.getId());
        for (EventMatch m : matches) {
            if ("AUTO_MATCHED".equals(m.getStatus()) || "MANUAL_MATCHED".equals(m.getStatus())) {
                SourceReport partner = m.getLeftReport().getId().equals(report.getId()) ? m.getRightReport() : m.getLeftReport();
                Optional<SourceReportRevision> partnerRevOpt = partner.getCurrentRevisionId() != null
                        ? revisionRepository.findById(partner.getCurrentRevisionId())
                        : revisionRepository.findFirstBySourceReportIdOrderByRevisionNumberDesc(partner.getId());

                if (partnerRevOpt.isPresent()) {
                    detail.setMatchedPartner(mapToItemDto(partnerRevOpt.get()));
                    detail.setMatchStatus(m.getStatus());
                    detail.setMatchReason(m.getDecisionReason());
                    detail.setMatchScore(m.getScore());
                    currentDto.setMatchStatus(m.getStatus());
                    currentDto.setMatchedEventId(partner.getSource().toLowerCase() + ":" + partner.getSourceEventId());
                    break;
                }
            }
        }

        return Optional.of(detail);
    }

    public StatusResponseDto getStatus() {
        Instant now = Instant.now();
        Instant afadFetch = statusTracker.getSourceState("AFAD").getLastSuccessfulFetchAt();
        Instant afadAttempt = statusTracker.getSourceState("AFAD").getLastAttemptAt();
        String afadFreshness = statusTracker.getFreshnessStatus("AFAD", now);

        Instant koeriFetch = statusTracker.getSourceState("KOERI").getLastSuccessfulFetchAt();
        Instant koeriAttempt = statusTracker.getSourceState("KOERI").getLastAttemptAt();
        String koeriFreshness = statusTracker.getFreshnessStatus("KOERI", now);

        Instant afadCoverageFrom = revisionRepository.findEarliestOccurredAtBySource("AFAD").orElse(null);
        Instant koeriCoverageFrom = revisionRepository.findEarliestOccurredAtBySource("KOERI").orElse(null);

        StatusResponseDto.CoverageDto afadCoverage = new StatusResponseDto.CoverageDto(
                afadCoverageFrom,
                afadFetch != null ? afadFetch : now,
                statusTracker.getSourceState("AFAD").isCoverageComplete()
        );

        StatusResponseDto.SourceStatusDto afadStatus = new StatusResponseDto.SourceStatusDto(
                "AFAD",
                afadFreshness,
                afadFetch,
                afadAttempt,
                afadCoverage
        );

        StatusResponseDto.CoverageDto koeriCoverage = new StatusResponseDto.CoverageDto(
                koeriCoverageFrom,
                koeriFetch != null ? koeriFetch : now,
                statusTracker.getSourceState("KOERI").isCoverageComplete()
        );

        StatusResponseDto.SourceStatusDto koeriStatus = new StatusResponseDto.SourceStatusDto(
                "KOERI",
                koeriFreshness,
                koeriFetch,
                koeriAttempt,
                koeriCoverage
        );

        return new StatusResponseDto("available", "live", List.of(afadStatus, koeriStatus));
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

        dto.setSolutionQuality(rev.getSolutionQuality());
        dto.setMagMd(rev.getMagMd());
        dto.setMagMl(rev.getMagMl());
        dto.setMagMw(rev.getMagMw());

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

    private static final Locale TR_LOCALE = Locale.forLanguageTag("tr-TR");

    private Expression<String> turkishFold(CriteriaBuilder cb, Expression<String> expr) {
        Expression<String> r1 = cb.function("replace", String.class, expr, cb.literal("İ"), cb.literal("i"));
        Expression<String> r2 = cb.function("replace", String.class, r1, cb.literal("I"), cb.literal("i"));
        Expression<String> r3 = cb.function("replace", String.class, r2, cb.literal("ı"), cb.literal("i"));
        return cb.lower(r3);
    }

    private String turkishFold(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("İ", "i")
                .replace("I", "i")
                .replace("ı", "i")
                .toLowerCase(TR_LOCALE)
                .toLowerCase(Locale.ROOT);
    }
}
