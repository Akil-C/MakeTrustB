package com.markettrust.seller.dto;

import com.markettrust.seller.entity.KycIdType;
import com.markettrust.seller.entity.KycStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class KycStatusDto {
    private Long id;
    private Long sellerId;
    private KycIdType idType;
    private String idNumber;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String pincode;
    private KycStatus status;
    private String rejectionReason;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;

    public KycStatusDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSellerId() { return sellerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }

    public KycIdType getIdType() { return idType; }
    public void setIdType(KycIdType idType) { this.idType = idType; }

    public String getIdNumber() { return idNumber; }
    public void setIdNumber(String idNumber) { this.idNumber = idNumber; }

    public String getAddressLine1() { return addressLine1; }
    public void setAddressLine1(String addressLine1) { this.addressLine1 = addressLine1; }

    public String getAddressLine2() { return addressLine2; }
    public void setAddressLine2(String addressLine2) { this.addressLine2 = addressLine2; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getPincode() { return pincode; }
    public void setPincode(String pincode) { this.pincode = pincode; }

    public KycStatus getStatus() { return status; }
    public void setStatus(KycStatus status) { this.status = status; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }

    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }

    public static KycStatusDtoBuilder builder() { return new KycStatusDtoBuilder(); }

    public static class KycStatusDtoBuilder {
        private Long id;
        private Long sellerId;
        private KycIdType idType;
        private String idNumber;
        private String addressLine1;
        private String addressLine2;
        private String city;
        private String state;
        private String pincode;
        private KycStatus status;
        private String rejectionReason;
        private LocalDateTime submittedAt;
        private LocalDateTime reviewedAt;

        public KycStatusDtoBuilder id(Long id) { this.id = id; return this; }
        public KycStatusDtoBuilder sellerId(Long sellerId) { this.sellerId = sellerId; return this; }
        public KycStatusDtoBuilder idType(KycIdType idType) { this.idType = idType; return this; }
        public KycStatusDtoBuilder idNumber(String idNumber) { this.idNumber = idNumber; return this; }
        public KycStatusDtoBuilder addressLine1(String addressLine1) { this.addressLine1 = addressLine1; return this; }
        public KycStatusDtoBuilder addressLine2(String addressLine2) { this.addressLine2 = addressLine2; return this; }
        public KycStatusDtoBuilder city(String city) { this.city = city; return this; }
        public KycStatusDtoBuilder state(String state) { this.state = state; return this; }
        public KycStatusDtoBuilder pincode(String pincode) { this.pincode = pincode; return this; }
        public KycStatusDtoBuilder status(KycStatus status) { this.status = status; return this; }
        public KycStatusDtoBuilder rejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; return this; }
        public KycStatusDtoBuilder submittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; return this; }
        public KycStatusDtoBuilder reviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; return this; }

        public KycStatusDto build() {
            KycStatusDto dto = new KycStatusDto();
            dto.setId(id);
            dto.setSellerId(sellerId);
            dto.setIdType(idType);
            dto.setIdNumber(idNumber);
            dto.setAddressLine1(addressLine1);
            dto.setAddressLine2(addressLine2);
            dto.setCity(city);
            dto.setState(state);
            dto.setPincode(pincode);
            dto.setStatus(status);
            dto.setRejectionReason(rejectionReason);
            dto.setSubmittedAt(submittedAt);
            dto.setReviewedAt(reviewedAt);
            return dto;
        }
    }
}
