package com.ag.deprem.controller;

import com.ag.deprem.model.dto.ArchiveManifestDto;
import com.ag.deprem.model.dto.BackfillResultDto;
import com.ag.deprem.service.AfadBackfillService;
import com.ag.deprem.service.ArchiveExporterService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/archive")
public class ArchiveController {

    private final ArchiveExporterService archiveExporterService;
    private final AfadBackfillService backfillService;

    public ArchiveController(ArchiveExporterService archiveExporterService,
                             AfadBackfillService backfillService) {
        this.archiveExporterService = archiveExporterService;
        this.backfillService = backfillService;
    }

    /**
     * Lists all published archive manifests with integrity metadata and checksums.
     */
    @GetMapping("/manifests")
    public ResponseEntity<List<ArchiveManifestDto>> listManifests() {
        return ResponseEntity.ok(archiveExporterService.listManifests());
    }

    /**
     * Gets a specific archive manifest by version name.
     */
    @GetMapping("/manifests/{archiveVersion}")
    public ResponseEntity<ArchiveManifestDto> getManifest(@PathVariable("archiveVersion") String archiveVersion) {
        return archiveExporterService.getManifest(archiveVersion)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Verifies SHA-256 integrity and record count of a shard on disk.
     */
    @PostMapping("/verify/{archiveVersion}")
    public ResponseEntity<Map<String, Object>> verifyArchive(@PathVariable("archiveVersion") String archiveVersion) {
        boolean verified = archiveExporterService.verifyArchive(archiveVersion);
        return ResponseEntity.ok(Map.of(
                "archiveVersion", archiveVersion,
                "verified", verified
        ));
    }

    /**
     * Exports a static JSON archive shard for a given time range.
     */
    @PostMapping("/export")
    public ResponseEntity<ArchiveManifestDto> exportArchive(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end,
            @RequestParam("version") String version) {
        ArchiveManifestDto manifest = archiveExporterService.exportArchive(start, end, version);
        return ResponseEntity.ok(manifest);
    }

    /**
     * Triggers historical AFAD backfill for a bounded time window.
     */
    @PostMapping("/backfill")
    public ResponseEntity<BackfillResultDto> triggerBackfill(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end) {
        BackfillResultDto result = backfillService.runBackfill(start, end);
        return ResponseEntity.ok(result);
    }
}
