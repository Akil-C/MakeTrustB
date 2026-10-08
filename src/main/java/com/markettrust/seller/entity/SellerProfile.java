package com.markettrust.seller.entity;

import com.markettrust.common.BaseEntity;
import com.markettrust.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "seller_profiles", indexes = {
        @Index(name = "idx_seller_profile_user_id", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SellerProfile extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @Column(name = "display_name", nullable = false, length = 150)
    private String displayName;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status", nullable = false, length = 30)
    private KycStatus kycStatus = KycStatus.PENDING;

    @Column(name = "trust_score", nullable = false)
    private Double trustScore = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(name = "seller_level", nullable = false, length = 20)
    private SellerLevel sellerLevel = SellerLevel.NEW;

    @Column(name = "response_rate")
    private Double responseRate = 0.0;

    @Column(name = "cancellation_rate")
    private Double cancellationRate = 0.0;

    @Column(name = "completed_orders", nullable = false)
    private Integer completedOrders = 0;

    @Column(name = "total_sales")
    private BigDecimal totalSales = BigDecimal.ZERO;

    @Column(name = "average_rating")
    private Double averageRating = 0.0;

    @Column(name = "review_count", nullable = false)
    private Integer reviewCount = 0;

    @Column(name = "is_verified", nullable = false)
    private Boolean isVerified = false;

    @Column(name = "seller_since")
    private LocalDateTime sellerSince = LocalDateTime.now();

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    public KycStatus getKycStatus() { return kycStatus; }
    public void setKycStatus(KycStatus kycStatus) { this.kycStatus = kycStatus; }

    public Double getTrustScore() { return trustScore != null ? trustScore : 0.0; }
    public void setTrustScore(Double trustScore) { this.trustScore = trustScore; }

    public SellerLevel getSellerLevel() { return sellerLevel; }
    public void setSellerLevel(SellerLevel sellerLevel) { this.sellerLevel = sellerLevel; }

    public Double getResponseRate() { return responseRate != null ? responseRate : 0.0; }
    public void setResponseRate(Double responseRate) { this.responseRate = responseRate; }

    public Double getCancellationRate() { return cancellationRate != null ? cancellationRate : 0.0; }
    public void setCancellationRate(Double cancellationRate) { this.cancellationRate = cancellationRate; }

    public Integer getCompletedOrders() { return completedOrders != null ? completedOrders : 0; }
    public void setCompletedOrders(Integer completedOrders) { this.completedOrders = completedOrders; }

    public BigDecimal getTotalSales() { return totalSales; }
    public void setTotalSales(BigDecimal totalSales) { this.totalSales = totalSales; }

    public Double getAverageRating() { return averageRating != null ? averageRating : 0.0; }
    public void setAverageRating(Double averageRating) { this.averageRating = averageRating; }

    public Integer getReviewCount() { return reviewCount != null ? reviewCount : 0; }
    public void setReviewCount(Integer reviewCount) { this.reviewCount = reviewCount; }

    public Boolean getIsVerified() { return Boolean.TRUE.equals(isVerified); }
    public void setIsVerified(Boolean isVerified) { this.isVerified = isVerified; }

    public LocalDateTime getSellerSince() { return sellerSince; }
    public void setSellerSince(LocalDateTime sellerSince) { this.sellerSince = sellerSince; }
}
