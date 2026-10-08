package com.markettrust.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Lightweight product projection used across Chat, Report, AI modules.
 */
public class ProductSummaryDto {

    private Long id;
    private String title;
    private BigDecimal priceInCredits;
    private String primaryImageUrl;
    private String status;
    private String condition;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public BigDecimal getPriceInCredits() { return priceInCredits; }
    public void setPriceInCredits(BigDecimal priceInCredits) { this.priceInCredits = priceInCredits; }

    public String getPrimaryImageUrl() { return primaryImageUrl; }
    public void setPrimaryImageUrl(String primaryImageUrl) { this.primaryImageUrl = primaryImageUrl; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }

    public static ProductSummaryDtoBuilder builder() {
        return new ProductSummaryDtoBuilder();
    }

    public static class ProductSummaryDtoBuilder {
        private Long id;
        private String title;
        private BigDecimal priceInCredits;
        private String primaryImageUrl;
        private String status;
        private String condition;

        public ProductSummaryDtoBuilder id(Long id) { this.id = id; return this; }
        public ProductSummaryDtoBuilder title(String title) { this.title = title; return this; }
        public ProductSummaryDtoBuilder priceInCredits(BigDecimal priceInCredits) { this.priceInCredits = priceInCredits; return this; }
        public ProductSummaryDtoBuilder primaryImageUrl(String primaryImageUrl) { this.primaryImageUrl = primaryImageUrl; return this; }
        public ProductSummaryDtoBuilder status(String status) { this.status = status; return this; }
        public ProductSummaryDtoBuilder condition(String condition) { this.condition = condition; return this; }

        public ProductSummaryDto build() {
            ProductSummaryDto dto = new ProductSummaryDto();
            dto.setId(id);
            dto.setTitle(title);
            dto.setPriceInCredits(priceInCredits);
            dto.setPrimaryImageUrl(primaryImageUrl);
            dto.setStatus(status);
            dto.setCondition(condition);
            return dto;
        }
    }
}
