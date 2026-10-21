package com.ag.deprem.service;

import com.ag.deprem.model.dto.FeltReportRequestDto;
import com.ag.deprem.model.dto.FeltReportSummaryDto;
import com.ag.deprem.model.dto.ProvinceFeltStatDto;
import com.ag.deprem.model.entity.EventMatch;
import com.ag.deprem.model.entity.FeltReport;
import com.ag.deprem.model.entity.SourceReport;
import com.ag.deprem.model.entity.SourceReportRevision;
import com.ag.deprem.repository.EventMatchRepository;
import com.ag.deprem.repository.FeltReportRepository;
import com.ag.deprem.repository.SourceReportRepository;
import com.ag.deprem.repository.SourceReportRevisionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
public class FeltReportService {

    private static final Logger log = LoggerFactory.getLogger(FeltReportService.class);
    private static final long MAX_HOURS_TO_REPORT = 48L;

    private final FeltReportRepository feltReportRepository;
    private final SourceReportRepository reportRepository;
    private final SourceReportRevisionRepository revisionRepository;
    private final EventMatchRepository matchRepository;
    private final String ipSalt;

    public FeltReportService(FeltReportRepository feltReportRepository,
                             SourceReportRepository reportRepository,
                             SourceReportRevisionRepository revisionRepository,
                             EventMatchRepository matchRepository,
                             @Value("${app.security.salt:deprem-felt-salt-2026}") String ipSalt) {
        this.feltReportRepository = feltReportRepository;
        this.reportRepository = reportRepository;
        this.revisionRepository = revisionRepository;
        this.matchRepository = matchRepository;
        this.ipSalt = ipSalt;
    }

    @Transactional
    public FeltReportSummaryDto submitReport(String eventIdStr, FeltReportRequestDto request, String clientIp) {
        SourceReport report = reportRepository.findReportByIdString(eventIdStr)
                .orElseThrow(() -> new NoSuchElementException("Deprem bulunamadı: " + eventIdStr));

        // Verify time window (< 48 hours from occurrence)
        SourceReportRevision currentRev = resolveCurrentRevision(report);
        if (currentRev != null && currentRev.getOccurredAt() != null) {
            long hoursSince = Duration.between(currentRev.getOccurredAt(), Instant.now()).toHours();
            if (hoursSince > MAX_HOURS_TO_REPORT) {
                throw new IllegalStateException("Bu deprem için bildirim süresi (" + MAX_HOURS_TO_REPORT + " saat) sona ermiştir.");
            }
        }

        String ipHash = hashClientIp(clientIp);

        // Find all linked reports in the unified event cluster (AFAD + Kandilli)
        Set<UUID> clusterReportIds = getClusterReportIds(report.getId());

        // Check if client has already submitted for any report in this cluster
        if (feltReportRepository.existsBySourceReportIdInAndIpHash(clusterReportIds, ipHash)) {
            throw new IllegalStateException("Bu deprem için zaten bildirimde bulundunuz.");
        }

        int intensity = calculateEmsIntensity(request);

        FeltReport entity = new FeltReport();
        entity.setId(UUID.randomUUID());
        entity.setSourceReport(report);
        entity.setReportedAt(Instant.now());
        entity.setCreatedAt(Instant.now());
        entity.setIpHash(ipHash);
        entity.setFeltLevel(request.getFeltLevel() != null ? request.getFeltLevel().toUpperCase().trim() : "MODERATE");
        entity.setMotionType(request.getMotionType() != null ? request.getMotionType().toUpperCase().trim() : null);
        entity.setReaction(request.getReaction() != null ? request.getReaction().toUpperCase().trim() : null);
        entity.setSoundHeard(request.getSoundHeard());
        entity.setDamageObserved(request.getDamageObserved() != null ? request.getDamageObserved().toUpperCase().trim() : "NONE");
        entity.setBuildingFloor(request.getBuildingFloor());

        // KVKK / GDPR compliance: Coarse coordinates rounded to 2 decimal places (~1.1 km)
        if (request.getLatitude() != null && request.getLongitude() != null) {
            entity.setLatitude(BigDecimal.valueOf(request.getLatitude()).setScale(2, RoundingMode.HALF_UP).doubleValue());
            entity.setLongitude(BigDecimal.valueOf(request.getLongitude()).setScale(2, RoundingMode.HALF_UP).doubleValue());
        }

        entity.setProvince(request.getProvince() != null ? request.getProvince().trim() : null);
        entity.setDistrict(request.getDistrict() != null ? request.getDistrict().trim() : null);
        entity.setCalculatedIntensity(intensity);

        feltReportRepository.save(entity);
        log.info("Saved felt report for event {} (intensity={}, province={})", report.getSourceEventId(), intensity, entity.getProvince());

        return getSummary(eventIdStr, clientIp);
    }

    @Transactional(readOnly = true)
    public FeltReportSummaryDto getSummary(String eventIdStr, String clientIp) {
        SourceReport report = reportRepository.findReportByIdString(eventIdStr)
                .orElseThrow(() -> new NoSuchElementException("Deprem bulunamadı: " + eventIdStr));

        Set<UUID> clusterReportIds = getClusterReportIds(report.getId());
        List<Object[]> intensityRows = feltReportRepository.getIntensitySummary(clusterReportIds);

        long totalReports = 0L;
        double avgIntensity = 0.0;
        if (!intensityRows.isEmpty()) {
            Object[] row = intensityRows.get(0);
            totalReports = row[0] instanceof Number n ? n.longValue() : 0L;
            avgIntensity = row[1] instanceof Number n ? n.doubleValue() : 0.0;
        }

        FeltReportSummaryDto summary = new FeltReportSummaryDto();
        summary.setTotalReports((int) totalReports);

        if (totalReports == 0L) {
            summary.setAverageIntensity(0.0);
            summary.setIntensityCategory("Bildirim Yok");
            summary.setDominantReaction("Henüz bildirim yapılmadı");
            summary.setAlreadyReportedByClient(false);
            return summary;
        }

        summary.setAverageIntensity(BigDecimal.valueOf(avgIntensity).setScale(1, RoundingMode.HALF_UP).doubleValue());
        summary.setIntensityCategory(getIntensityCategory(avgIntensity));

        // Dominant reaction via SQL aggregation
        List<Object[]> reactionRows = feltReportRepository.findDominantReaction(clusterReportIds, PageRequest.of(0, 1));
        String topReaction = !reactionRows.isEmpty() && reactionRows.get(0)[0] != null
                ? reactionRows.get(0)[0].toString()
                : "SURPRISED";
        summary.setDominantReaction(formatReactionText(topReaction));

        // Group by province via SQL aggregation (Top 10)
        List<Object[]> provRows = feltReportRepository.findProvinceStats(clusterReportIds, PageRequest.of(0, 10));
        List<ProvinceFeltStatDto> provStats = provRows.stream().map(row -> {
            String prov = row[0] != null ? row[0].toString() : "";
            long cnt = row[1] instanceof Number n ? n.longValue() : 0L;
            double pAvg = row[2] instanceof Number n ? n.doubleValue() : 0.0;
            return new ProvinceFeltStatDto(
                    prov,
                    (int) cnt,
                    BigDecimal.valueOf(pAvg).setScale(1, RoundingMode.HALF_UP).doubleValue()
            );
        }).toList();
        summary.setReportsByProvince(provStats);

        // Check if current client already submitted via indexed query
        if (clientIp != null && !clientIp.isBlank()) {
            String ipHash = hashClientIp(clientIp);
            boolean already = feltReportRepository.existsBySourceReportIdInAndIpHash(clusterReportIds, ipHash);
            summary.setAlreadyReportedByClient(already);
        }

        return summary;
    }

    /**
     * EMS-98 / European Macroseismic Scale Intensity Estimator (1 to 10)
     */
    public int calculateEmsIntensity(FeltReportRequestDto req) {
        double score = 3.0; // default baseline

        String level = req.getFeltLevel() != null ? req.getFeltLevel().toUpperCase() : "MODERATE";
        switch (level) {
            case "NOT_FELT" -> score = 1.0;
            case "WEAK" -> score = 2.5;
            case "MODERATE" -> score = 4.0;
            case "STRONG" -> score = 6.0;
            case "VIOLENT" -> score = 8.0;
            default -> score = 3.5;
        }

        if (req.getMotionType() != null) {
            switch (req.getMotionType().toUpperCase()) {
                case "SLOW_SWAYING" -> score += 0.2;
                case "RAPID_SHAKING" -> score += 0.5;
                case "JOLTING" -> score += 0.7;
                case "VIBRATION" -> score += 0.1;
            }
        }

        if (req.getReaction() != null) {
            switch (req.getReaction().toUpperCase()) {
                case "SURPRISED" -> score += 0.3;
                case "FRIGHTENED" -> score += 0.8;
                case "RAN_OUTSIDE" -> score += 1.2;
            }
        }

        if (Boolean.TRUE.equals(req.getSoundHeard())) {
            score += 0.3;
        }

        if (req.getDamageObserved() != null) {
            switch (req.getDamageObserved().toUpperCase()) {
                case "HAIRLINE_CRACKS" -> score += 1.5;
                case "OBJECTS_TOPPLED" -> score += 1.0;
                case "STRUCTURAL" -> score += 2.5;
            }
        }

        // Higher floors experience amplified lateral motion; normalize to ground observation
        if (req.getBuildingFloor() != null && req.getBuildingFloor() > 1) {
            double floorDamp = Math.min(0.2 * (req.getBuildingFloor() - 1), 1.5);
            score -= floorDamp;
        }

        int finalScore = (int) Math.round(score);
        return Math.max(1, Math.min(10, finalScore));
    }

    public String getIntensityCategory(double intensity) {
        if (intensity < 2.5) return "EMS-I / II (Fark Edilmedi veya Çok Hafif)";
        if (intensity < 4.5) return "EMS-III / IV (Hafif / Belirgin Sarsıntı)";
        if (intensity < 6.0) return "EMS-V (Güçlü Sarsıntı)";
        if (intensity < 7.5) return "EMS-VI (Hafif Hasar / Şiddetli Sarsıntı)";
        return "EMS-VII+ (Ağır Sarsıntı ve Hasar)";
    }

    private String formatReactionText(String reaction) {
        return switch (reaction.toUpperCase()) {
            case "RAN_OUTSIDE" -> "Pek çok kişi dışarı çıktı";
            case "FRIGHTENED" -> "Korku ve panik hissedildi";
            case "SURPRISED" -> "Şaşkınlık ve hafif tedirginlik";
            case "NO_REACTION" -> "Sakin karşılandı / hissedilmedi";
            default -> "Hissedildi";
        };
    }

    private Set<UUID> getClusterReportIds(UUID rootReportId) {
        Set<UUID> ids = new HashSet<>();
        ids.add(rootReportId);

        List<EventMatch> matches = matchRepository.findAllByReportId(rootReportId);
        for (EventMatch m : matches) {
            if ("AUTO_MATCHED".equals(m.getStatus()) || "MANUAL_MATCHED".equals(m.getStatus())) {
                ids.add(m.getLeftReport().getId());
                ids.add(m.getRightReport().getId());
            }
        }
        return ids;
    }

    private String hashClientIp(String ip) {
        String input = (ip != null ? ip.trim() : "unknown") + ":" + ipSalt;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    private SourceReportRevision resolveCurrentRevision(SourceReport report) {
        if (report.getCurrentRevisionId() != null) {
            return revisionRepository.findById(report.getCurrentRevisionId()).orElse(null);
        }
        return revisionRepository.findFirstBySourceReportIdOrderByRevisionNumberDesc(report.getId()).orElse(null);
    }
}
