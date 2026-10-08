package com.markettrust.report.repository;

import com.markettrust.report.entity.Report;
import com.markettrust.report.entity.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link Report} entity.
 */
@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    Page<Report> findByStatusOrderByCreatedAtDesc(ReportStatus status, Pageable pageable);

    Page<Report> findByReportedUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    long countByReportedUserIdAndStatus(Long userId, ReportStatus status);

    long countByStatus(ReportStatus status);
}
