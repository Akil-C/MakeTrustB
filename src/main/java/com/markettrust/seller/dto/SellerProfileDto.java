package com.markettrust.seller.dto;

import com.markettrust.seller.entity.KycStatus;
import com.markettrust.seller.entity.SellerLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class SellerProfileDto {
    private Long userId;
    private String displayName;
    private String bio;
    private String city;
    private String state;
    private SellerLevel sellerLevel;
    private Double trustScore;
    private Boolean isVerified;
    private KycStatus kycStatus;
    private Double averageRating;
    private Integer reviewCount;
    private Integer completedOrders;
    private Integer totalSales;
    private Double responseRate;
    private Double cancellationRate;
    private LocalDateTime sellerSince;
    private String profileImageUrl;

    public SellerProfileDto() {}

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public SellerLevel getSellerLevel() { return sellerLevel; }
    public void setSellerLevel(SellerLevel sellerLevel) { this.sellerLevel = sellerLevel; }

    public Double getTrustScore() { return trustScore; }
    public void setTrustScore(Double trustScore) { this.trustScore = trustScore; }

    public Boolean getIsVerified() { return isVerified; }
    public void setIsVerified(Boolean isVerified) { this.isVerified = isVerified; }

    public KycStatus getKycStatus() { return kycStatus; }
    public void setKycStatus(KycStatus kycStatus) { this.kycStatus = kycStatus; }

    public Double getAverageRating() { return averageRating; }
    public void setAverageRating(Double averageRating) { this.averageRating = averageRating; }

    public Integer getReviewCount() { return reviewCount; }
    public void setReviewCount(Integer reviewCount) { this.reviewCount = reviewCount; }

    public Integer getCompletedOrders() { return completedOrders; }
    public void setCompletedOrders(Integer completedOrders) { this.completedOrders = completedOrders; }

    public Integer getTotalSales() { return totalSales; }
    public void setTotalSales(Integer totalSales) { this.totalSales = totalSales; }

    public Double getResponseRate() { return responseRate; }
    public void setResponseRate(Double responseRate) { this.responseRate = responseRate; }

    public Double getCancellationRate() { return cancellationRate; }
    public void setCancellationRate(Double cancellationRate) { this.cancellationRate = cancellationRate; }

    public LocalDateTime getSellerSince() { return sellerSince; }
    public void setSellerSince(LocalDateTime sellerSince) { this.sellerSince = sellerSince; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    public static SellerProfileDtoBuilder builder() { return new SellerProfileDtoBuilder(); }

    public static class SellerProfileDtoBuilder {
        private Long userId;
        private String displayName;
        private String bio;
        private String city;
        private String state;
        private SellerLevel sellerLevel;
        private Double trustScore;
        private Boolean isVerified;
        private KycStatus kycStatus;
        private Double averageRating;
        private Integer reviewCount;
        private Integer completedOrders;
        private Integer totalSales;
        private Double responseRate;
        private Double cancellationRate;
        private LocalDateTime sellerSince;
        private String profileImageUrl;

        public SellerProfileDtoBuilder userId(Long userId) { this.userId = userId; return this; }
        public SellerProfileDtoBuilder displayName(String displayName) { this.displayName = displayName; return this; }
        public SellerProfileDtoBuilder bio(String bio) { this.bio = bio; return this; }
        public SellerProfileDtoBuilder city(String city) { this.city = city; return this; }
        public SellerProfileDtoBuilder state(String state) { this.state = state; return this; }
        public SellerProfileDtoBuilder sellerLevel(SellerLevel sellerLevel) { this.sellerLevel = sellerLevel; return this; }
        public SellerProfileDtoBuilder trustScore(Double trustScore) { this.trustScore = trustScore; return this; }
        public SellerProfileDtoBuilder isVerified(Boolean isVerified) { this.isVerified = isVerified; return this; }
        public SellerProfileDtoBuilder kycStatus(KycStatus kycStatus) { this.kycStatus = kycStatus; return this; }
        public SellerProfileDtoBuilder averageRating(Double averageRating) { this.averageRating = averageRating; return this; }
        public SellerProfileDtoBuilder reviewCount(Integer reviewCount) { this.reviewCount = reviewCount; return this; }
        public SellerProfileDtoBuilder completedOrders(Integer completedOrders) { this.completedOrders = completedOrders; return this; }
        public SellerProfileDtoBuilder totalSales(Integer totalSales) { this.totalSales = totalSales; return this; }
        public SellerProfileDtoBuilder responseRate(Double responseRate) { this.responseRate = responseRate; return this; }
        public SellerProfileDtoBuilder cancellationRate(Double cancellationRate) { this.cancellationRate = cancellationRate; return this; }
        public SellerProfileDtoBuilder sellerSince(LocalDateTime sellerSince) { this.sellerSince = sellerSince; return this; }
        public SellerProfileDtoBuilder profileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; return this; }

        public SellerProfileDto build() {
            SellerProfileDto dto = new SellerProfileDto();
            dto.setUserId(userId);
            dto.setDisplayName(displayName);
            dto.setBio(bio);
            dto.setCity(city);
            dto.setState(state);
            dto.setSellerLevel(sellerLevel);
            dto.setTrustScore(trustScore);
            dto.setIsVerified(isVerified);
            dto.setKycStatus(kycStatus);
            dto.setAverageRating(averageRating);
            dto.setReviewCount(reviewCount);
            dto.setCompletedOrders(completedOrders);
            dto.setTotalSales(totalSales);
            dto.setResponseRate(responseRate);
            dto.setCancellationRate(cancellationRate);
            dto.setSellerSince(sellerSince);
            dto.setProfileImageUrl(profileImageUrl);
            return dto;
        }
    }
}
