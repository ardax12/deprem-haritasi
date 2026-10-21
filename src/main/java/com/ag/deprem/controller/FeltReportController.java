package com.ag.deprem.controller;

import com.ag.deprem.model.dto.FeltReportRequestDto;
import com.ag.deprem.model.dto.FeltReportSummaryDto;
import com.ag.deprem.service.FeltReportService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/earthquakes/{id}/felt-reports")
public class FeltReportController {

    private final FeltReportService feltReportService;

    public FeltReportController(FeltReportService feltReportService) {
        this.feltReportService = feltReportService;
    }

    @PostMapping
    public ResponseEntity<FeltReportSummaryDto> submitFeltReport(
            @PathVariable String id,
            @Valid @RequestBody FeltReportRequestDto request,
            HttpServletRequest servletRequest) {

        String clientIp = extractClientIp(servletRequest);
        FeltReportSummaryDto summary = feltReportService.submitReport(id, request, clientIp);
        return ResponseEntity.status(HttpStatus.CREATED).body(summary);
    }

    @GetMapping("/summary")
    public ResponseEntity<FeltReportSummaryDto> getFeltSummary(
            @PathVariable String id,
            HttpServletRequest servletRequest) {

        String clientIp = extractClientIp(servletRequest);
        FeltReportSummaryDto summary = feltReportService.getSummary(id, clientIp);
        return ResponseEntity.ok(summary);
    }

    private String extractClientIp(HttpServletRequest request) {
        // 1. Nginx sets X-Real-IP from $remote_addr (authoritative behind reverse proxy)
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }

        // 2. Cloudflare Connecting IP
        String cfConnectingIp = request.getHeader("CF-Connecting-IP");
        if (cfConnectingIp != null && !cfConnectingIp.isBlank()) {
            return cfConnectingIp.trim();
        }

        // 3. Fallback to X-Forwarded-For: take the rightmost IP (closest trusted reverse-proxy hop)
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            String[] hops = xForwardedFor.split(",");
            return hops[hops.length - 1].trim();
        }

        return request.getRemoteAddr();
    }
}
