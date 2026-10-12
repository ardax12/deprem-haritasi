package com.ag.deprem.controller;

import com.ag.deprem.model.dto.StatusResponseDto;
import com.ag.deprem.service.EarthquakeQueryService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/status")
public class StatusController {

    private final EarthquakeQueryService queryService;

    public StatusController(EarthquakeQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping
    public ResponseEntity<StatusResponseDto> getStatus() {
        StatusResponseDto status = queryService.getStatus();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(30, TimeUnit.SECONDS).cachePublic())
                .body(status);
    }
}
