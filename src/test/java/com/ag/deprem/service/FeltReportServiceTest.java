package com.ag.deprem.service;

import com.ag.deprem.model.dto.FeltReportRequestDto;
import com.ag.deprem.model.dto.FeltReportSummaryDto;
import com.ag.deprem.model.entity.EventMatch;
import com.ag.deprem.model.entity.FeltReport;
import com.ag.deprem.model.entity.SourceReport;
import com.ag.deprem.model.entity.SourceReportRevision;
import com.ag.deprem.repository.EventMatchRepository;
import com.ag.deprem.repository.FeltReportRepository;
import com.ag.deprem.repository.SourceReportRepository;
import com.ag.deprem.repository.SourceReportRevisionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeltReportServiceTest {

    @Mock
    private FeltReportRepository feltReportRepository;

    @Mock
    private SourceReportRepository reportRepository;

    @Mock
    private SourceReportRevisionRepository revisionRepository;

    @Mock
    private EventMatchRepository matchRepository;

    private FeltReportService service;

    @BeforeEach
    void setUp() {
        service = new FeltReportService(feltReportRepository, reportRepository, revisionRepository, matchRepository, "test-salt-123");
    }

    @Test
    @DisplayName("calculateEmsIntensity should accurately score low, moderate and violent perceptions")
    void testCalculateEmsIntensity() {
        FeltReportRequestDto notFelt = new FeltReportRequestDto();
        notFelt.setFeltLevel("NOT_FELT");
        assertThat(service.calculateEmsIntensity(notFelt)).isEqualTo(1);

        FeltReportRequestDto moderate = new FeltReportRequestDto();
        moderate.setFeltLevel("MODERATE");
        moderate.setMotionType("RAPID_SHAKING"); // +0.5
        moderate.setReaction("SURPRISED");       // +0.3
        // 4.0 + 0.5 + 0.3 = 4.8 -> rounds to 5
        assertThat(service.calculateEmsIntensity(moderate)).isEqualTo(5);

        // Test high-floor dampening
        FeltReportRequestDto highFloor = new FeltReportRequestDto();
        highFloor.setFeltLevel("STRONG"); // 6.0
        highFloor.setBuildingFloor(8);    // floor > 1 -> damp = min(0.2 * 7, 1.5) = 1.4 -> 4.6 -> 5
        assertThat(service.calculateEmsIntensity(highFloor)).isEqualTo(5);
    }

    @Test
    @DisplayName("submitReport should reject submission if client already reported for this event")
    void testRejectDuplicateSubmission() {
        UUID reportId = UUID.randomUUID();
        SourceReport report = new SourceReport(reportId, "AFAD", "12345", Instant.now(), Instant.now());

        SourceReportRevision rev = new SourceReportRevision();
        rev.setId(UUID.randomUUID());
        rev.setSourceReport(report);
        rev.setOccurredAt(Instant.now().minus(2, ChronoUnit.HOURS));
        report.setCurrentRevisionId(rev.getId());

        when(reportRepository.findBySourceAndSourceEventId("AFAD", "12345")).thenReturn(Optional.of(report));
        when(revisionRepository.findById(rev.getId())).thenReturn(Optional.of(rev));
        when(matchRepository.findAllByReportId(reportId)).thenReturn(Collections.emptyList());
        when(feltReportRepository.existsBySourceReportIdAndIpHash(eq(reportId), anyString())).thenReturn(true);

        FeltReportRequestDto req = new FeltReportRequestDto();
        req.setFeltLevel("MODERATE");

        assertThatThrownBy(() -> service.submitReport("afad:12345", req, "192.168.1.50"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("zaten bildirimde bulundunuz");
    }

    @Test
    @DisplayName("getSummary should aggregate reports across matched AFAD and Kandilli twin events")
    void testSummaryAggregatesAcrossCluster() {
        UUID afadId = UUID.randomUUID();
        UUID koeriId = UUID.randomUUID();

        SourceReport afadReport = new SourceReport(afadId, "AFAD", "afad-100", Instant.now(), Instant.now());
        SourceReport koeriReport = new SourceReport(koeriId, "KOERI", "koeri-200", Instant.now(), Instant.now());

        EventMatch match = new EventMatch();
        match.setLeftReport(afadReport);
        match.setRightReport(koeriReport);
        match.setStatus("AUTO_MATCHED");

        when(reportRepository.findBySourceAndSourceEventId("AFAD", "afad-100")).thenReturn(Optional.of(afadReport));
        when(matchRepository.findAllByReportId(afadId)).thenReturn(List.of(match));

        FeltReport r1 = new FeltReport();
        r1.setId(UUID.randomUUID());
        r1.setSourceReport(afadReport);
        r1.setCalculatedIntensity(4);
        r1.setProvince("İzmir");
        r1.setReaction("SURPRISED");

        FeltReport r2 = new FeltReport();
        r2.setId(UUID.randomUUID());
        r2.setSourceReport(koeriReport); // Submitted on Kandilli card
        r2.setCalculatedIntensity(6);
        r2.setProvince("İzmir");
        r2.setReaction("FRIGHTENED");

        when(feltReportRepository.findReportsForReports(anyCollection())).thenReturn(List.of(r1, r2));

        FeltReportSummaryDto summary = service.getSummary("afad:afad-100", "192.168.1.1");

        assertThat(summary.getTotalReports()).isEqualTo(2);
        assertThat(summary.getAverageIntensity()).isEqualTo(5.0);
        assertThat(summary.getReportsByProvince()).hasSize(1);
        assertThat(summary.getReportsByProvince().get(0).getProvince()).isEqualTo("İzmir");
        assertThat(summary.getReportsByProvince().get(0).getCount()).isEqualTo(2);
    }
}
