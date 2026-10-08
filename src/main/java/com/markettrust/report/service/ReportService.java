package com.markettrust.report.service;

import com.markettrust.common.dto.ProductSummaryDto;
import com.markettrust.common.dto.UserSummaryDto;
import com.markettrust.exception.ResourceNotFoundException;
import com.markettrust.notification.service.NotificationService;
import com.markettrust.product.entity.Product;
import com.markettrust.product.repository.ProductRepository;
import com.markettrust.report.dto.CreateReportRequest;
import com.markettrust.report.dto.ReportDto;
import com.markettrust.report.dto.ResolveReportRequest;
import com.markettrust.report.entity.*;
import com.markettrust.report.repository.ReportRepository;
import com.markettrust.user.entity.User;
import com.markettrust.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Service handling the full lifecycle of user/content reports.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    /** High-severity reasons that immediately bump risk score. */
    private static final Set<ReportReason> HIGH_SEVERITY_REASONS = Set.of(
            ReportReason.SCAM, ReportReason.OFF_PLATFORM_PAYMENT, ReportReason.FRAUD,
            ReportReason.SUSPICIOUS_BEHAVIOR
    );

    private final ReportRepository    reportRepository;
    private final UserRepository      userRepository;
    private final ProductRepository   productRepository;
    private final NotificationService notificationService;

    // ---------------------------------------------------------------------------
    // Create
    // ---------------------------------------------------------------------------

    /**
     * Creates a new report and calculates a preliminary AI risk score.
     *
     * @param reporterId the authenticated user filing the report
     * @param req        the report payload
     * @return persisted {@link ReportDto}
     */
    @Transactional
    public ReportDto createReport(Long reporterId, CreateReportRequest req) {
        // Must report someone/something
        if (req.getReportedUserId() == null && req.getReportedProductId() == null
                && req.getReportedReviewId() == null && req.getReportedMessageId() == null) {
            throw new IllegalArgumentException("At least one target (user, product, review, or message) must be specified");
        }

        // Prevent self-reporting
        if (reporterId.equals(req.getReportedUserId())) {
            throw new IllegalArgumentException("You cannot report yourself");
        }

        Report report = new Report();
        report.setReporterId(reporterId);
        report.setReportedUserId(req.getReportedUserId());
        report.setReportedProductId(req.getReportedProductId());
        report.setReportedReviewId(req.getReportedReviewId());
        report.setReportedMessageId(req.getReportedMessageId());
        report.setReason(req.getReason());
        report.setDescription(req.getDescription());
        report.setStatus(ReportStatus.OPEN);

        // Determine risk score and priority
        double riskScore = calculateRiskScore(req.getReportedUserId(), req.getReason());
        report.setAiRiskScore(riskScore);
        report.setPriority(priorityFromScore(riskScore));

        report = reportRepository.save(report);
        log.info("Report created id={} reporter={} priority={} score={}",
                report.getId(), reporterId, report.getPriority(), riskScore);

        // Notify admins for high-risk reports
        if (report.getPriority() == ReportPriority.HIGH) {
            notificationService.createNotification(
                    0L, // Admin notification user (0 = system / admin group)
                    "HIGH_RISK_REPORT",
                    "High-Risk Report Submitted",
                    "A high-risk report (id=" + report.getId() + ") requires immediate attention.",
                    report.getId(), "REPORT"
            );
        }

        return toDto(report);
    }

    // ---------------------------------------------------------------------------
    // Query
    // ---------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<ReportDto> getReports(ReportStatus status, Pageable pageable) {
        Page<Report> page = (status != null)
                ? reportRepository.findByStatusOrderByCreatedAtDesc(status, pageable)
                : reportRepository.findAll(pageable);
        return page.map(this::toDto);
    }

    @Transactional(readOnly = true)
    public ReportDto getReportById(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report", "id", reportId));
        return toDto(report);
    }

    @Transactional(readOnly = true)
    public long getUserReportCount(Long userId) {
        return reportRepository.countByReportedUserIdAndStatus(userId, ReportStatus.OPEN);
    }

    // ---------------------------------------------------------------------------
    // Admin resolution
    // ---------------------------------------------------------------------------

    @Transactional
    public ReportDto resolveReport(Long adminId, Long reportId, ResolveReportRequest req) {
        Report report = loadReport(reportId);
        report.setStatus(req.getStatus());
        report.setResolvedBy(adminId);
        report.setResolutionNotes(req.getNotes());
        report.setResolvedAt(LocalDateTime.now());
        report = reportRepository.save(report);
        log.info("Report {} resolved by admin {} with status {}", reportId, adminId, req.getStatus());
        return toDto(report);
    }

    @Transactional
    public ReportDto rejectReport(Long adminId, Long reportId, String notes) {
        Report report = loadReport(reportId);
        report.setStatus(ReportStatus.REJECTED);
        report.setResolvedBy(adminId);
        report.setResolutionNotes(notes);
        report.setResolvedAt(LocalDateTime.now());
        report = reportRepository.save(report);
        log.info("Report {} rejected by admin {}", reportId, adminId);
        return toDto(report);
    }

    // ---------------------------------------------------------------------------
    // Private helpers
    // ---------------------------------------------------------------------------

    /**
     * Deterministic risk-score calculation based on prior open-report count
     * and the severity of the reported reason.
     * Range: 0.0–1.0
     */
    private double calculateRiskScore(Long targetUserId, ReportReason reason) {
        double base = HIGH_SEVERITY_REASONS.contains(reason) ? 0.6 : 0.3;

        if (targetUserId != null) {
            long priorReports = reportRepository.countByReportedUserIdAndStatus(targetUserId, ReportStatus.OPEN);
            // Each prior open report adds 0.1, capped at 0.4 bonus
            double priorBonus = Math.min(0.4, priorReports * 0.1);
            base = Math.min(1.0, base + priorBonus);
        }
        return base;
    }

    private ReportPriority priorityFromScore(double score) {
        if (score >= 0.7) return ReportPriority.HIGH;
        if (score >= 0.4) return ReportPriority.MEDIUM;
        return ReportPriority.LOW;
    }

    private Report loadReport(Long reportId) {
        return reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report", "id", reportId));
    }

    private ReportDto toDto(Report report) {
        UserSummaryDto reporterDto = userRepository.findById(report.getReporterId())
                .map(this::toUserSummary).orElse(null);

        UserSummaryDto reportedUserDto = report.getReportedUserId() != null
                ? userRepository.findById(report.getReportedUserId()).map(this::toUserSummary).orElse(null)
                : null;

        ProductSummaryDto reportedProductDto = report.getReportedProductId() != null
                ? productRepository.findById(report.getReportedProductId()).map(this::toProductSummary).orElse(null)
                : null;

        return ReportDto.builder()
                .id(report.getId())
                .reporter(reporterDto)
                .reportedUser(reportedUserDto)
                .reportedProduct(reportedProductDto)
                .reportedReviewId(report.getReportedReviewId())
                .reportedMessageId(report.getReportedMessageId())
                .reason(report.getReason())
                .description(report.getDescription())
                .status(report.getStatus())
                .priority(report.getPriority())
                .aiRiskScore(report.getAiRiskScore())
                .resolvedBy(report.getResolvedBy())
                .resolutionNotes(report.getResolutionNotes())
                .resolvedAt(report.getResolvedAt())
                .createdAt(report.getCreatedAt())
                .build();
    }

    private UserSummaryDto toUserSummary(User u) {
        return UserSummaryDto.builder()
                .id(u.getId())
                .name(u.getName())
                .profileImageUrl(u.getProfileImageUrl())
                .build();
    }

    private ProductSummaryDto toProductSummary(Product p) {
        return ProductSummaryDto.builder()
                .id(p.getId())
                .title(p.getTitle())
                .priceInCredits(p.getPriceInCredits() != null ? BigDecimal.valueOf(p.getPriceInCredits()) : null)
                .status(p.getStatus() != null ? p.getStatus().name() : null)
                .condition(p.getCondition() != null ? p.getCondition().name() : null)
                .build();
    }
}
