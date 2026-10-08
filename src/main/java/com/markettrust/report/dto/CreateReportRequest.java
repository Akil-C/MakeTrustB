package com.markettrust.report.dto;

import com.markettrust.report.entity.ReportReason;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Payload for filing a new report.
 * At least one of reportedUserId, reportedProductId, reportedReviewId, or reportedMessageId
 * must be provided (validated at service layer).
 */
@Data
public class CreateReportRequest {

    @NotNull(message = "Report reason is required")
    private ReportReason reason;

    @NotBlank(message = "Description is required")
    private String description;

    private Long reportedUserId;
    private Long reportedProductId;
    private Long reportedReviewId;
    private Long reportedMessageId;

    public ReportReason getReason() { return reason; }
    public void setReason(ReportReason reason) { this.reason = reason; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getReportedUserId() { return reportedUserId; }
    public void setReportedUserId(Long reportedUserId) { this.reportedUserId = reportedUserId; }

    public Long getReportedProductId() { return reportedProductId; }
    public void setReportedProductId(Long reportedProductId) { this.reportedProductId = reportedProductId; }

    public Long getReportedReviewId() { return reportedReviewId; }
    public void setReportedReviewId(Long reportedReviewId) { this.reportedReviewId = reportedReviewId; }

    public Long getReportedMessageId() { return reportedMessageId; }
    public void setReportedMessageId(Long reportedMessageId) { this.reportedMessageId = reportedMessageId; }
}
