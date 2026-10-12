package com.ag.deprem.controller;

import com.ag.deprem.model.dto.EarthquakeDetailDto;
import com.ag.deprem.model.dto.EarthquakesListResponseDto;
import com.ag.deprem.service.EarthquakeQueryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/earthquakes")
public class EarthquakeController {

    private final EarthquakeQueryService queryService;

    public EarthquakeController(EarthquakeQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping
    public ResponseEntity<EarthquakesListResponseDto> listEarthquakes(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end,
            @RequestParam(required = false) Double minMagnitude,
            @RequestParam(required = false) Double maxMagnitude,
            @RequestParam(required = false) String magnitudeType,
            @RequestParam(required = false) Double minDepthKm,
            @RequestParam(required = false) Double maxDepthKm,
            @RequestParam(required = false) String bbox,
            @RequestParam(required = false) String place,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int pageSize,
            @RequestParam(required = false) String sort) {

        EarthquakesListResponseDto response = queryService.queryEarthquakes(
                start, end, minMagnitude, maxMagnitude, magnitudeType,
                minDepthKm, maxDepthKm, bbox, place, page, pageSize, sort
        );

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS).cachePublic())
                .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EarthquakeDetailDto> getEarthquakeDetail(@PathVariable String id) {
        EarthquakeDetailDto detail = queryService.getEarthquakeDetail(id)
                .orElseThrow(() -> new NoSuchElementException("Earthquake not found with ID: " + id));

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS).cachePublic())
                .body(detail);
    }
}
