package com.ag.deprem.controller;

import com.ag.deprem.model.dto.AfadRawEventDto;
import com.ag.deprem.model.dto.FeltReportRequestDto;
import com.ag.deprem.repository.FeltReportRepository;
import com.ag.deprem.repository.SourceReportRepository;
import com.ag.deprem.service.IngestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class FeltReportControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private IngestionService ingestionService;

    @Autowired
    private SourceReportRepository reportRepository;

    @Autowired
    private FeltReportRepository feltReportRepository;

    @Autowired
    private com.ag.deprem.repository.SourceReportRevisionRepository revisionRepository;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws Exception {
        feltReportRepository.deleteAll();
        reportRepository.deleteAll();
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        objectMapper = new ObjectMapper();

        try (InputStream is = getClass().getResourceAsStream("/fixtures/afad/afad_sample_events.json")) {
            List<AfadRawEventDto> samples = objectMapper.readValue(is, new TypeReference<>() {});
            ingestionService.ingestAfadEvents(samples);
        }

        // Make fixture event recent (< 48 hours) so reporting window is open
        reportRepository.findBySourceAndSourceEventId("AFAD", "731199").ifPresent(report -> {
            revisionRepository.findById(report.getCurrentRevisionId()).ifPresent(rev -> {
                rev.setOccurredAt(java.time.Instant.now().minus(2, java.time.temporal.ChronoUnit.HOURS));
                revisionRepository.save(rev);
            });
        });
    }

    @Test
    void testSubmitFeltReportAndRetrieveSummary() throws Exception {
        // Sample fixture event ID from afad_sample_events.json
        String eventId = "afad:731199";

        FeltReportRequestDto request = new FeltReportRequestDto();
        request.setFeltLevel("STRONG");
        request.setMotionType("RAPID_SHAKING");
        request.setReaction("FRIGHTENED");
        request.setSoundHeard(true);
        request.setBuildingFloor(3);
        request.setProvince("İzmir");
        request.setDistrict("Bornova");
        request.setLatitude(38.46);
        request.setLongitude(27.21);

        String json = objectMapper.writeValueAsString(request);

        // 1. Submit report
        mockMvc.perform(post("/api/v1/earthquakes/" + eventId + "/felt-reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .header("X-Forwarded-For", "203.0.113.195"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalReports").value(1))
                .andExpect(jsonPath("$.averageIntensity").isNumber())
                .andExpect(jsonPath("$.reportsByProvince[0].province").value("İzmir"));

        // 2. Fetch summary
        mockMvc.perform(get("/api/v1/earthquakes/" + eventId + "/felt-reports/summary")
                        .header("X-Forwarded-For", "203.0.113.195"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReports").value(1))
                .andExpect(jsonPath("$.alreadyReportedByClient").value(true));

        // 3. Duplicate submission from same IP should return 409 Conflict
        mockMvc.perform(post("/api/v1/earthquakes/" + eventId + "/felt-reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .header("X-Forwarded-For", "203.0.113.195"))
                .andExpect(status().isConflict());
    }
}
