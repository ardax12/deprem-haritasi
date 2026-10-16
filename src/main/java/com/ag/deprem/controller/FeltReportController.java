package com.ag.deprem.controller;

import com.ag.deprem.model.dto.FeltReportRequestDto;
import com.ag.deprem.model.dto.FeltReportSummaryDto;
import com.ag.deprem.service.FeltReportService;
import jakarta.servlet.http.HttpServletRequest;
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
            @RequestBody FeltReportRequestDto request,
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
        String cfConnectingIp = request.getHeader("CF-Connecting-IP");
        if (cfConnectingIp != null && !cfConnectingIp.isBlank()) {
            return cfConnectingIp.trim();
        }

        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            // First IP in the comma-separated chain is the client IP
            int idx = xForwardedFor.indexOf(',');
            if (idx > 0) {
                return xForwardedFor.substring(0, idx).trim();
            }
            return xForwardedFor.trim();
        }

        return request.getRemoteAddr();
    }
}
