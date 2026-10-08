package com.markettrust.seller.entity;

import com.markettrust.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "seller_trust_scores", indexes = {
        @Index(name = "idx_seller_trust_score_seller_id", columnList = "seller_profile_id")
})
@Getter
@Setter
@NoArgsConstructor
public class SellerTrustScore extends BaseEntity {

    @Column(name = "seller_profile_id", nullable = false, unique = true)
    private Long sellerProfileId;

    @Column(name = "total_score", nullable = false)
    private Double totalScore = 0.0;

    @Column(name = "kyc_points", nullable = false)
    private Double kycPoints = 0.0;

    @Column(name = "order_points", nullable = false)
    private Double orderPoints = 0.0;

    @Column(name = "review_points", nullable = false)
    private Double reviewPoints = 0.0;

    @Column(name = "penalty_points", nullable = false)
    private Double penaltyPoints = 0.0;

    @Column(name = "cancellation_penalty", nullable = false)
    private Double cancellationPenalty = 0.0;

    @Column(name = "dispute_penalty", nullable = false)
    private Double disputePenalty = 0.0;

    @Column(name = "report_penalty", nullable = false)
    private Double reportPenalty = 0.0;

    @Column(name = "ai_risk_penalty", nullable = false)
    private Double aiRiskPenalty = 0.0;

    @Column(name = "last_calculated")
    private LocalDateTime lastCalculated;
}
