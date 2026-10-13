package com.ag.deprem.controller;

import com.ag.deprem.model.dto.ArchiveManifestDto;
import com.ag.deprem.model.dto.BackfillResultDto;
import com.ag.deprem.service.AfadBackfillService;
import com.ag.deprem.service.ArchiveExporterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class ArchiveControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @MockitoBean
    private ArchiveExporterService archiveExporterService;

    @MockitoBean
    private AfadBackfillService backfillService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void testListManifests() throws Exception {
        ArchiveManifestDto manifest = new ArchiveManifestDto(
                UUID.randomUUID(),
                "archive-2026-09",
                Instant.now().minusSeconds(86400 * 30),
                Instant.now(),
                150L,
                Instant.now(),
                "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2",
                "1.0",
                "AFAD",
                true,
                "/archive/archive-2026-09.json"
        );

        when(archiveExporterService.listManifests()).thenReturn(List.of(manifest));

        mockMvc.perform(get("/api/v1/archive/manifests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].archiveVersion").value("archive-2026-09"))
                .andExpect(jsonPath("$[0].recordCount").value(150))
                .andExpect(jsonPath("$[0].verified").value(true));
    }

    @Test
    void testGetManifestNotFound() throws Exception {
        when(archiveExporterService.getManifest("non-existent")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/archive/manifests/non-existent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testVerifyArchive() throws Exception {
        when(archiveExporterService.verifyArchive("archive-2026-09")).thenReturn(true);

        mockMvc.perform(post("/api/v1/archive/verify/archive-2026-09"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true));
    }

    @Test
    void testTriggerBackfill() throws Exception {
        BackfillResultDto result = new BackfillResultDto(
                Instant.now().minusSeconds(86400),
                Instant.now(),
                25, 20, 5, 0, 0, 1, true, "OK"
        );

        when(backfillService.runBackfill(any(), any())).thenReturn(result);

        mockMvc.perform(post("/api/v1/archive/backfill")
                        .param("start", "2026-09-01T00:00:00Z")
                        .param("end", "2026-09-02T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.totalEventsFetched").value(25));
    }
}
