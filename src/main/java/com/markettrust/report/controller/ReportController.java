package com.markettrust.report.controller;

import com.markettrust.report.dto.CreateReportRequest;
import com.markettrust.report.dto.ReportDto;
import com.markettrust.report.dto.ResolveReportRequest;
import com.markettrust.report.entity.ReportStatus;
import com.markettrust.report.service.ReportService;
import com.markettrust.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for report filing (users) and moderation (admins).
 */
@RestController
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    // ---------------------------------------------------------------------------
    // User endpoints
    // ---------------------------------------------------------------------------

    /**
     * POST /api/reports
     * File a new report.
     */
    @PostMapping("/api/reports")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReportDto> createReport(
            @Valid @RequestBody CreateReportRequest req) {

        Long reporterId = SecurityUtils.getCurrentUserId();
        ReportDto dto   = reportService.createReport(reporterId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    // ---------------------------------------------------------------------------
    // Admin endpoints
    // ---------------------------------------------------------------------------

    /**
     * GET /api/admin/reports?status=OPEN&page=0&size=20
     * List all reports filtered by status.
     */
    @GetMapping("/api/admin/reports")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<ReportDto>> getReports(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(reportService.getReports(status, pageable));
    }

    /**
     * GET /api/admin/reports/{id}
     * Retrieve a single report by ID.
     */
    @GetMapping("/api/admin/reports/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReportDto> getReport(@PathVariable Long id) {
        return ResponseEntity.ok(reportService.getReportById(id));
    }

    /**
     * PUT /api/admin/reports/{id}/resolve
     * Resolve a report.
     */
    @PutMapping("/api/admin/reports/{id}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReportDto> resolveReport(
            @PathVariable Long id,
            @Valid @RequestBody ResolveReportRequest req) {

        Long adminId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(reportService.resolveReport(adminId, id, req));
    }

    /**
     * PUT /api/admin/reports/{id}/reject
     * Reject a report.
     */
    @PutMapping("/api/admin/reports/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReportDto> rejectReport(
            @PathVariable Long id,
            @RequestParam(defaultValue = "") String notes) {

        Long adminId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(reportService.rejectReport(adminId, id, notes));
    }
}
