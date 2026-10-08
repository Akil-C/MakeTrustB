package com.markettrust.report.dto;

import com.markettrust.common.dto.ProductSummaryDto;
import com.markettrust.common.dto.UserSummaryDto;
import com.markettrust.report.entity.ReportPriority;
import com.markettrust.report.entity.ReportReason;
import com.markettrust.report.entity.ReportStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO projection of a {@link com.markettrust.report.entity.Report}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportDto {

    private Long id;
    private UserSummaryDto reporter;

    /** Nullable — populated when a user was reported. */
    private UserSummaryDto reportedUser;

    /** Nullable — populated when a product was reported. */
    private ProductSummaryDto reportedProduct;

    private Long reportedReviewId;
    private Long reportedMessageId;

    private ReportReason reason;
    private String description;
    private ReportStatus status;
    private ReportPriority priority;
    private Double aiRiskScore;

    private Long resolvedBy;
    private String resolutionNotes;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
}
