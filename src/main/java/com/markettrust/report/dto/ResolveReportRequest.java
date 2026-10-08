package com.markettrust.report.dto;

import com.markettrust.report.entity.ReportStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Admin payload for resolving or rejecting a report.
 */
@Data
public class ResolveReportRequest {

    @NotNull(message = "Resolution status is required")
    private ReportStatus status;

    @NotBlank(message = "Resolution notes are required")
    private String notes;
}
