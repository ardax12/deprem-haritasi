package com.ag.deprem.controller;

import com.ag.deprem.model.dto.AfadRawEventDto;
import com.ag.deprem.service.IngestionService;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class EarthquakeControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private IngestionService ingestionService;

    private ObjectMapper objectMapper;

    @Autowired
    private com.ag.deprem.repository.SourceReportRepository reportRepository;

    @BeforeEach
    void setUp() throws Exception {
        reportRepository.deleteAll();
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilters(new ShallowEtagHeaderFilter())
                .build();
        objectMapper = new ObjectMapper();
        try (InputStream is = getClass().getResourceAsStream("/fixtures/afad/afad_sample_events.json")) {
            List<AfadRawEventDto> samples = objectMapper.readValue(is, new TypeReference<>() {});
            ingestionService.ingestAfadEvents(samples);
        }
    }

    @Test
    void testListEarthquakesReturns200AndETag() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/earthquakes")
                        .param("start", "2026-10-11T00:00:00Z")
                        .param("end", "2026-10-13T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"))
                .andExpect(header().string("Cache-Control", "max-age=60, public"))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.freshness.source").value("AFAD"))
                .andExpect(jsonPath("$.page.size").exists())
                .andReturn();

        String etag = result.getResponse().getHeader("ETag");
        assertNotNull(etag);

        // Test If-None-Match returns 304 Not Modified
        mockMvc.perform(get("/api/v1/earthquakes")
                        .param("start", "2026-10-11T00:00:00Z")
                        .param("end", "2026-10-13T00:00:00Z")
                        .header("If-None-Match", etag))
                .andExpect(status().isNotModified());
    }

    @Test
    void testFilterByMinMagnitude() throws Exception {
        // Event 731199 has mag 2.1, Event 731198 has mag 1.4
        mockMvc.perform(get("/api/v1/earthquakes")
                        .param("start", "2026-10-11T00:00:00Z")
                        .param("end", "2026-10-13T00:00:00Z")
                        .param("minMagnitude", "2.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value("afad:731199"));
    }

    @Test
    void testFilterByBbox() throws Exception {
        // Event 731199 is around lat 37.77, lon 36.15 (Adana)
        // Bbox covering Adana: west=35.0, south=36.0, east=37.0, north=39.0
        mockMvc.perform(get("/api/v1/earthquakes")
                        .param("start", "2026-10-11T00:00:00Z")
                        .param("end", "2026-10-13T00:00:00Z")
                        .param("bbox", "35.0,36.0,37.0,39.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value("afad:731199"));
    }

    @Test
    void testFilterByPlace() throws Exception {
        mockMvc.perform(get("/api/v1/earthquakes")
                        .param("start", "2026-10-11T00:00:00Z")
                        .param("end", "2026-10-13T00:00:00Z")
                        .param("place", "Adana"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value("afad:731199"));
    }

    @Test
    void testFilterByPlaceTurkishCharacters() throws Exception {
        AfadRawEventDto izmirEvent = new AfadRawEventDto();
        izmirEvent.setEventId("731201");
        izmirEvent.setDate("2026-10-12T05:00:00");
        izmirEvent.setLatitude("38.4237");
        izmirEvent.setLongitude("27.1428");
        izmirEvent.setDepth("10.0");
        izmirEvent.setMagnitude("3.2");
        izmirEvent.setType("Mw");
        izmirEvent.setLocation("Bornova (İzmir)");
        izmirEvent.setProvince("İzmir");
        izmirEvent.setDistrict("Bornova");
        izmirEvent.setCountry("Türkiye");

        AfadRawEventDto igdirEvent = new AfadRawEventDto();
        igdirEvent.setEventId("731202");
        igdirEvent.setDate("2026-10-12T06:00:00");
        igdirEvent.setLatitude("39.9196");
        igdirEvent.setLongitude("44.0450");
        igdirEvent.setDepth("8.5");
        igdirEvent.setMagnitude("2.5");
        igdirEvent.setType("ML");
        igdirEvent.setLocation("Aralık (Iğdır)");
        igdirEvent.setProvince("Iğdır");
        igdirEvent.setDistrict("Aralık");
        igdirEvent.setCountry("Türkiye");

        ingestionService.ingestAfadEvents(List.of(izmirEvent, igdirEvent));

        // Test searching İzmir with various Turkish and Latin letter variations
        for (String search : List.of("İzmir", "izmir", "İZMİR", "IZMIR")) {
            mockMvc.perform(get("/api/v1/earthquakes")
                            .param("start", "2026-10-11T00:00:00Z")
                            .param("end", "2026-10-13T00:00:00Z")
                            .param("place", search))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items[0].id").value("afad:731201"));
        }

        // Test searching Iğdır with various Turkish and Latin letter variations
        for (String search : List.of("Iğdır", "ığdır", "IĞDIR")) {
            mockMvc.perform(get("/api/v1/earthquakes")
                            .param("start", "2026-10-11T00:00:00Z")
                            .param("end", "2026-10-13T00:00:00Z")
                            .param("place", search))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items[0].id").value("afad:731202"));
        }
    }

    @Test
    void testFilterByMagnitudeType() throws Exception {
        AfadRawEventDto mwEvent = new AfadRawEventDto();
        mwEvent.setEventId("731203");
        mwEvent.setDate("2026-10-12T07:00:00");
        mwEvent.setLatitude("38.0");
        mwEvent.setLongitude("27.0");
        mwEvent.setDepth("10.0");
        mwEvent.setMagnitude("4.5");
        mwEvent.setType("Mw");
        mwEvent.setLocation("Ege Denizi");
        mwEvent.setCountry("Türkiye");

        ingestionService.ingestAfadEvents(List.of(mwEvent));

        // Filter ML should find existing ML events and not the Mw event
        mockMvc.perform(get("/api/v1/earthquakes")
                        .param("start", "2026-10-11T00:00:00Z")
                        .param("end", "2026-10-13T00:00:00Z")
                        .param("magnitudeType", "ML"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2));

        // Filter Mw should find the Mw event
        mockMvc.perform(get("/api/v1/earthquakes")
                        .param("start", "2026-10-11T00:00:00Z")
                        .param("end", "2026-10-13T00:00:00Z")
                        .param("magnitudeType", "Mw"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value("afad:731203"));

        // Filter MD should find 0 events
        mockMvc.perform(get("/api/v1/earthquakes")
                        .param("start", "2026-10-11T00:00:00Z")
                        .param("end", "2026-10-13T00:00:00Z")
                        .param("magnitudeType", "MD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    void testInvalidBboxReturns400ProblemDetail() throws Exception {
        mockMvc.perform(get("/api/v1/earthquakes")
                        .param("bbox", "invalid,coords"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Geçersiz Parametre"))
                .andExpect(jsonPath("$.detail").value("bbox must contain 4 comma-separated values: west,south,east,north"));
    }

    @Test
    void testGetEarthquakeDetailSuccessAndNotFound() throws Exception {
        // Success
        mockMvc.perform(get("/api/v1/earthquakes/afad:731199"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.current.id").value("afad:731199"))
                .andExpect(jsonPath("$.history").isArray());

        // Not Found
        mockMvc.perform(get("/api/v1/earthquakes/afad:non-existent"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Kayıt Bulunamadı"));
    }

    @Test
    void testGetStatusReturns200() throws Exception {
        mockMvc.perform(get("/api/v1/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.application").value("available"))
                .andExpect(jsonPath("$.mode").value("live"))
                .andExpect(jsonPath("$.sources[0].name").value("AFAD"))
                .andExpect(jsonPath("$.sources[0].status").exists());
    }

    @Test
    void testTypeMismatchReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/earthquakes")
                        .param("minMagnitude", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Parametre Tipi Uyuşmazlığı"));
    }

    @Test
    void testMalformedDateReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/earthquakes")
                        .param("start", "not-a-valid-date"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Parametre Tipi Uyuşmazlığı"));
    }

    @Test
    void testOverlongPlaceReturns400() throws Exception {
        String longPlace = "A".repeat(105);
        mockMvc.perform(get("/api/v1/earthquakes")
                        .param("place", longPlace))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Geçersiz Parametre"));
    }
}
