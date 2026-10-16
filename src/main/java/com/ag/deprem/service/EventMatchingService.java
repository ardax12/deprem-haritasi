package com.ag.deprem.service;

import com.ag.deprem.model.entity.*;
import com.ag.deprem.repository.EventGroupMemberRepository;
import com.ag.deprem.repository.EventGroupRepository;
import com.ag.deprem.repository.EventMatchRepository;
import com.ag.deprem.repository.SourceReportRevisionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
public class EventMatchingService {

    private static final Logger log = LoggerFactory.getLogger(EventMatchingService.class);
    private static final String ALGORITHM_VERSION = "2.0-composite";

    private final EventMatchRepository matchRepository;
    private final EventGroupRepository groupRepository;
    private final EventGroupMemberRepository memberRepository;
    private final SourceReportRevisionRepository revisionRepository;

    private final long maxTimeDeltaSeconds;
    private final double maxDistanceKm;
    private final double autoMatchThreshold;
    private final double scoreMargin;

    public EventMatchingService(EventMatchRepository matchRepository,
                                EventGroupRepository groupRepository,
                                EventGroupMemberRepository memberRepository,
                                SourceReportRevisionRepository revisionRepository,
                                @Value("${app.matching.time-window-seconds:120}") long maxTimeDeltaSeconds,
                                @Value("${app.matching.max-distance-km:50.0}") double maxDistanceKm,
                                @Value("${app.matching.auto-match-threshold:0.70}") double autoMatchThreshold,
                                @Value("${app.matching.score-margin:0.10}") double scoreMargin) {
        this.matchRepository = matchRepository;
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
        this.revisionRepository = revisionRepository;
        this.maxTimeDeltaSeconds = maxTimeDeltaSeconds;
        this.maxDistanceKm = maxDistanceKm;
        this.autoMatchThreshold = autoMatchThreshold;
        this.scoreMargin = scoreMargin;
    }

    public static class CandidateEvaluation {
        private final SourceReportRevision otherRevision;
        private final long timeDeltaSeconds;
        private final double distanceKm;
        private final Double depthDeltaKm;
        private final Double magnitudeDelta;
        private final double score;

        public CandidateEvaluation(SourceReportRevision otherRevision, long timeDeltaSeconds, double distanceKm,
                                   Double depthDeltaKm, Double magnitudeDelta, double score) {
            this.otherRevision = otherRevision;
            this.timeDeltaSeconds = timeDeltaSeconds;
            this.distanceKm = distanceKm;
            this.depthDeltaKm = depthDeltaKm;
            this.magnitudeDelta = magnitudeDelta;
            this.score = score;
        }

        public SourceReportRevision getOtherRevision() { return otherRevision; }
        public long getTimeDeltaSeconds() { return timeDeltaSeconds; }
        public double getDistanceKm() { return distanceKm; }
        public Double getDepthDeltaKm() { return depthDeltaKm; }
        public Double getMagnitudeDelta() { return magnitudeDelta; }
        public double getScore() { return score; }
    }

    /**
     * Evaluates matches for a newly created or updated SourceReportRevision against other sources.
     */
    @Transactional
    public void evaluateMatchesForReport(SourceReport report, SourceReportRevision currentRev) {
        if (report == null || currentRev == null) {
            return;
        }

        Instant occurredAt = currentRev.getOccurredAt();
        Instant windowStart = occurredAt.minusSeconds(maxTimeDeltaSeconds);
        Instant windowEnd = occurredAt.plusSeconds(maxTimeDeltaSeconds);

        // Fetch current revisions of reports from any source within the time window
        List<SourceReportRevision> windowRevisions = revisionRepository.findAllCurrentRevisionsBetween(windowStart, windowEnd);

        List<CandidateEvaluation> candidates = new ArrayList<>();

        for (SourceReportRevision otherRev : windowRevisions) {
            SourceReport otherReport = otherRev.getSourceReport();

            // Disallow matching report from the same source or same report ID
            if (otherReport.getId().equals(report.getId()) ||
                otherReport.getSource().equalsIgnoreCase(report.getSource())) {
                continue;
            }

            long timeDelta = Math.abs(Duration.between(occurredAt, otherRev.getOccurredAt()).toSeconds());
            if (timeDelta > maxTimeDeltaSeconds) {
                continue;
            }

            double distance = haversineDistanceKm(
                    currentRev.getLatitude(), currentRev.getLongitude(),
                    otherRev.getLatitude(), otherRev.getLongitude()
            );

            if (distance > maxDistanceKm) {
                continue;
            }

            Double depthDelta = null;
            if (currentRev.getDepthKm() != null && otherRev.getDepthKm() != null) {
                depthDelta = Math.abs(currentRev.getDepthKm() - otherRev.getDepthKm());
            }

            Double magDelta = computeComparableMagnitudeDelta(currentRev, otherRev);

            double score = computeScore(timeDelta, distance, depthDelta, magDelta);
            candidates.add(new CandidateEvaluation(otherRev, timeDelta, distance, depthDelta, magDelta, score));
        }

        if (candidates.isEmpty()) {
            return;
        }

        // Sort candidates descending by score
        candidates.sort(Comparator.comparingDouble(CandidateEvaluation::getScore).reversed());

        CandidateEvaluation bestCandidate = candidates.get(0);
        boolean marginSatisfied = true;
        if (candidates.size() > 1) {
            double secondScore = candidates.get(1).getScore();
            if ((bestCandidate.getScore() - secondScore) < scoreMargin) {
                marginSatisfied = false;
            }
        }

        // Mutual best match check: verify if report is also best candidate for otherReport
        SourceReportRevision partnerRev = bestCandidate.getOtherRevision();
        SourceReport partnerReport = partnerRev.getSourceReport();

        boolean mutualBest = isMutualBestMatch(partnerReport, partnerRev, report, currentRev, bestCandidate.getScore());

        for (CandidateEvaluation candidate : candidates) {
            SourceReport otherRep = candidate.getOtherRevision().getSourceReport();
            boolean isBest = otherRep.getId().equals(partnerReport.getId());

            String status;
            String reason;

            if (isBest && mutualBest && marginSatisfied && candidate.getScore() >= autoMatchThreshold) {
                status = "AUTO_MATCHED";
                reason = String.format(Locale.ROOT, "Otomatik eşleşme (Skor: %.2f, Zaman farkı: %d sn, Mesafe: %.1f km)",
                        candidate.getScore(), candidate.getTimeDeltaSeconds(), candidate.getDistanceKm());
            } else if (candidate.getScore() >= 0.40) {
                status = "REVIEW_REQUIRED";
                reason = isBest
                        ? (mutualBest ? "Skor marjı yetersiz veya eşiğe yakın" : "Karşılıklı en iyi aday eşleşmedi")
                        : "İkincil aday";
            } else {
                status = "CANDIDATE";
                reason = "Aday puanı düşük";
            }

            saveOrUpdateMatch(report, currentRev, otherRep, candidate.getOtherRevision(), candidate, status, reason);

            if ("AUTO_MATCHED".equals(status)) {
                linkReportsInEventGroup(report, currentRev, otherRep, candidate.getOtherRevision());
            }
        }
    }

    private boolean isMutualBestMatch(SourceReport partnerReport, SourceReportRevision partnerRev,
                                      SourceReport targetReport, SourceReportRevision targetRev,
                                      double targetScore) {
        Instant partnerTime = partnerRev.getOccurredAt();
        List<SourceReportRevision> candidatesForPartner = revisionRepository.findAllCurrentRevisionsBetween(
                partnerTime.minusSeconds(maxTimeDeltaSeconds),
                partnerTime.plusSeconds(maxTimeDeltaSeconds)
        );

        double highestScore = -1.0;
        UUID bestReportId = null;

        for (SourceReportRevision rev : candidatesForPartner) {
            if (rev.getSourceReport().getSource().equalsIgnoreCase(partnerReport.getSource())) {
                continue;
            }

            long td = Math.abs(Duration.between(partnerTime, rev.getOccurredAt()).toSeconds());
            if (td > maxTimeDeltaSeconds) continue;

            double dist = haversineDistanceKm(partnerRev.getLatitude(), partnerRev.getLongitude(), rev.getLatitude(), rev.getLongitude());
            if (dist > maxDistanceKm) continue;

            Double depthDelta = (partnerRev.getDepthKm() != null && rev.getDepthKm() != null)
                    ? Math.abs(partnerRev.getDepthKm() - rev.getDepthKm()) : null;
            Double magDelta = computeComparableMagnitudeDelta(partnerRev, rev);

            double s = computeScore(td, dist, depthDelta, magDelta);
            if (s > highestScore) {
                highestScore = s;
                bestReportId = rev.getSourceReport().getId();
            }
        }

        return targetReport.getId().equals(bestReportId);
    }

    private void saveOrUpdateMatch(SourceReport reportA, SourceReportRevision revA,
                                   SourceReport reportB, SourceReportRevision revB,
                                   CandidateEvaluation eval, String status, String reason) {
        // Enforce canonical ordering: UUID ordering
        SourceReport leftRep = reportA.getId().compareTo(reportB.getId()) <= 0 ? reportA : reportB;
        SourceReport rightRep = leftRep == reportA ? reportB : reportA;
        UUID leftRevId = leftRep == reportA ? revA.getId() : revB.getId();
        UUID rightRevId = leftRep == reportA ? revB.getId() : revA.getId();

        Instant now = Instant.now();
        Optional<EventMatch> existingOpt = matchRepository.findMatchBetweenReports(leftRep.getId(), rightRep.getId());
        if (existingOpt.isPresent()) {
            EventMatch existing = existingOpt.get();
            if ("MANUAL".equalsIgnoreCase(existing.getDecisionType())
                    || "MANUAL_MATCHED".equals(existing.getStatus())
                    || "REJECTED".equals(existing.getStatus())) {
                log.debug("Preserving manual match decision ({}) between {} and {}",
                        existing.getStatus(), leftRep.getId(), rightRep.getId());
                return;
            }
        }

        EventMatch match = existingOpt.orElseGet(() -> {
            EventMatch m = new EventMatch();
            m.setId(UUID.randomUUID());
            m.setLeftReport(leftRep);
            m.setRightReport(rightRep);
            m.setCreatedAt(now);
            return m;
        });

        match.setAlgorithmVersion(ALGORITHM_VERSION);
        match.setTimeDeltaSeconds(eval.getTimeDeltaSeconds());
        match.setDistanceKm(eval.getDistanceKm());
        match.setDepthDeltaKm(eval.getDepthDeltaKm());
        match.setMagnitudeDelta(eval.getMagnitudeDelta());
        match.setScore(eval.getScore());
        match.setStatus(status);
        match.setDecisionType("AUTO");
        match.setDecisionReason(reason);
        match.setLeftRevisionId(leftRevId);
        match.setRightRevisionId(rightRevId);
        match.setUpdatedAt(now);

        matchRepository.save(match);
    }

    private void linkReportsInEventGroup(SourceReport reportA, SourceReportRevision revA,
                                         SourceReport reportB, SourceReportRevision revB) {
        Optional<EventGroupMember> memberAOpt = memberRepository.findBySourceReportId(reportA.getId());
        Optional<EventGroupMember> memberBOpt = memberRepository.findBySourceReportId(reportB.getId());

        Instant now = Instant.now();

        if (memberAOpt.isPresent() && memberBOpt.isPresent()) {
            // Both already belong to a group
            return;
        }

        EventGroup targetGroup;
        if (memberAOpt.isPresent()) {
            targetGroup = memberAOpt.get().getEventGroup();
            boolean sameSourceExists = memberRepository.findByEventGroupId(targetGroup.getId()).stream()
                    .anyMatch(m -> m.getSourceReport().getSource().equalsIgnoreCase(reportB.getSource()));
            if (!sameSourceExists) {
                createGroupMember(targetGroup, reportB, revB, now);
            }
        } else if (memberBOpt.isPresent()) {
            targetGroup = memberBOpt.get().getEventGroup();
            boolean sameSourceExists = memberRepository.findByEventGroupId(targetGroup.getId()).stream()
                    .anyMatch(m -> m.getSourceReport().getSource().equalsIgnoreCase(reportA.getSource()));
            if (!sameSourceExists) {
                createGroupMember(targetGroup, reportA, revA, now);
            }
        } else {
            // Create a new unified EventGroup
            targetGroup = new EventGroup();
            targetGroup.setId(UUID.randomUUID());
            targetGroup.setCreatedAt(now);
            targetGroup.setUpdatedAt(now);
            targetGroup.setStatus("AUTO_CONFIRMED");

            // Pick preferred parameters (AFAD preferred for location/time if present, or earlier time)
            SourceReportRevision prefRev = revA.getSourceReport().getSource().equalsIgnoreCase("AFAD") ? revA : revB;
            targetGroup.setTitle(prefRev.getLocationText() != null ? prefRev.getLocationText() : "Bilinmeyen Bölge");
            targetGroup.setOccurredAt(prefRev.getOccurredAt());
            targetGroup.setLatitude(prefRev.getLatitude());
            targetGroup.setLongitude(prefRev.getLongitude());
            targetGroup.setPreferredMagnitudeValue(prefRev.getMagnitudeValue());
            targetGroup.setPreferredMagnitudeType(prefRev.getMagnitudeType());

            targetGroup = groupRepository.save(targetGroup);

            createGroupMember(targetGroup, reportA, revA, now);
            createGroupMember(targetGroup, reportB, revB, now);
        }
    }

    private void createGroupMember(EventGroup group, SourceReport report, SourceReportRevision rev, Instant now) {
        EventGroupMember member = new EventGroupMember();
        member.setId(UUID.randomUUID());
        member.setEventGroup(group);
        member.setSourceReport(report);
        member.setSourceRevisionId(rev.getId());
        member.setMembershipType("AUTO");
        member.setCreatedAt(now);
        memberRepository.save(member);
    }

    /**
     * Re-evaluates matches when a revision changes by marking previous matches as SUPERSEDED.
     */
    @Transactional
    public void handleRevisionChange(SourceReport report, SourceReportRevision newRevision) {
        List<EventMatch> existingMatches = matchRepository.findAllByReportId(report.getId());
        Instant now = Instant.now();
        for (EventMatch m : existingMatches) {
            if ("AUTO_MATCHED".equals(m.getStatus()) || "CANDIDATE".equals(m.getStatus()) || "REVIEW_REQUIRED".equals(m.getStatus())) {
                m.setStatus("SUPERSEDED");
                m.setDecisionReason("Kaynak raporu güncellendi (Yeni Revizyon #" + newRevision.getRevisionNumber() + ")");
                m.setUpdatedAt(now);
                matchRepository.save(m);
            }
        }

        // Clean up automatic group membership if it existed so stale groupings are resolved
        Optional<EventGroupMember> memberOpt = memberRepository.findBySourceReportId(report.getId());
        if (memberOpt.isPresent()) {
            EventGroupMember member = memberOpt.get();
            if ("AUTO".equalsIgnoreCase(member.getMembershipType())) {
                EventGroup group = member.getEventGroup();
                memberRepository.delete(member);
                List<EventGroupMember> remaining = memberRepository.findByEventGroupId(group.getId());
                if (remaining.isEmpty() || (remaining.size() == 1 && remaining.get(0).getId().equals(member.getId()))) {
                    groupRepository.delete(group);
                }
            }
        }

        evaluateMatchesForReport(report, newRevision);
    }

    private double computeScore(long timeDelta, double distanceKm, Double depthDelta, Double magDelta) {
        double timeScore = Math.max(0.0, 1.0 - ((double) timeDelta / maxTimeDeltaSeconds));
        double distScore = Math.max(0.0, 1.0 - (distanceKm / maxDistanceKm));

        double baseScore = (0.60 * timeScore) + (0.40 * distScore);

        if (magDelta != null) {
            if (magDelta <= 0.5) {
                baseScore += 0.05;
            } else if (magDelta > 1.5) {
                baseScore -= 0.10;
            }
        }

        if (depthDelta != null && depthDelta <= 10.0) {
            baseScore += 0.03;
        }

        return Math.clamp(baseScore, 0.0, 1.0);
    }

    public static double haversineDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0088; // Earth's mean radius in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private Double computeComparableMagnitudeDelta(SourceReportRevision revA, SourceReportRevision revB) {
        if (revA == null || revB == null) {
            return null;
        }
        if (revA.getMagMw() != null && revB.getMagMw() != null) {
            return Math.abs(revA.getMagMw() - revB.getMagMw());
        } else if (revA.getMagMl() != null && revB.getMagMl() != null) {
            return Math.abs(revA.getMagMl() - revB.getMagMl());
        } else if (revA.getMagMd() != null && revB.getMagMd() != null) {
            return Math.abs(revA.getMagMd() - revB.getMagMd());
        } else if (revA.getMagnitudeType() != null && revB.getMagnitudeType() != null
                && revA.getMagnitudeType().equalsIgnoreCase(revB.getMagnitudeType())
                && revA.getMagnitudeValue() != null && revB.getMagnitudeValue() != null) {
            return Math.abs(revA.getMagnitudeValue() - revB.getMagnitudeValue());
        }
        return null;
    }
}
