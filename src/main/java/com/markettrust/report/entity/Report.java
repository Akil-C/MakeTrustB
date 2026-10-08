package com.markettrust.report.entity;

import com.markettrust.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "reports", indexes = {
        @Index(name = "idx_report_reporter_id", columnList = "reporter_id"),
        @Index(name = "idx_report_status", columnList = "status"),
        @Index(name = "idx_report_priority", columnList = "priority")
})
@Getter
@Setter
@NoArgsConstructor
public class Report extends BaseEntity {

    @Column(name = "reporter_id", nullable = false)
    private Long reporterId;

    @Column(name = "reported_user_id")
    private Long reportedUserId;

    @Column(name = "reported_product_id")
    private Long reportedProductId;

    @Column(name = "reported_review_id")
    private Long reportedReviewId;

    @Column(name = "reported_message_id")
    private Long reportedMessageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 40)
    private ReportReason reason;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReportStatus status = ReportStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 10)
    private ReportPriority priority = ReportPriority.MEDIUM;

    @Column(name = "ai_risk_score")
    private Double aiRiskScore;

    @Column(name = "resolved_by")
    private Long resolvedBy;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    public Long getReporterId() { return reporterId; }
    public void setReporterId(Long reporterId) { this.reporterId = reporterId; }

    public Long getReportedUserId() { return reportedUserId; }
    public void setReportedUserId(Long reportedUserId) { this.reportedUserId = reportedUserId; }

    public Long getReportedProductId() { return reportedProductId; }
    public void setReportedProductId(Long reportedProductId) { this.reportedProductId = reportedProductId; }

    public Long getReportedReviewId() { return reportedReviewId; }
    public void setReportedReviewId(Long reportedReviewId) { this.reportedReviewId = reportedReviewId; }

    public Long getReportedMessageId() { return reportedMessageId; }
    public void setReportedMessageId(Long reportedMessageId) { this.reportedMessageId = reportedMessageId; }

    public ReportReason getReason() { return reason; }
    public void setReason(ReportReason reason) { this.reason = reason; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public ReportStatus getStatus() { return status; }
    public void setStatus(ReportStatus status) { this.status = status; }

    public ReportPriority getPriority() { return priority; }
    public void setPriority(ReportPriority priority) { this.priority = priority; }

    public Double getAiRiskScore() { return aiRiskScore; }
    public void setAiRiskScore(Double aiRiskScore) { this.aiRiskScore = aiRiskScore; }

    public Long getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(Long resolvedBy) { this.resolvedBy = resolvedBy; }

    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
}
