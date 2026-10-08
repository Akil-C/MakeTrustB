package com.markettrust.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class ProductImageDto {
    private Long id;
    private String url;
    private String publicId;
    private Boolean isPrimary;
    private Integer sortOrder;

    public ProductImageDto() {}

    public ProductImageDto(Long id, String url, String publicId, Boolean isPrimary, Integer sortOrder) {
        this.id = id;
        this.url = url;
        this.publicId = publicId;
        this.isPrimary = isPrimary;
        this.sortOrder = sortOrder;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getPublicId() { return publicId; }
    public void setPublicId(String publicId) { this.publicId = publicId; }

    public Boolean getIsPrimary() { return isPrimary; }
    public void setIsPrimary(Boolean isPrimary) { this.isPrimary = isPrimary; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public static ProductImageDtoBuilder builder() { return new ProductImageDtoBuilder(); }

    public static class ProductImageDtoBuilder {
        private Long id;
        private String url;
        private String publicId;
        private Boolean isPrimary;
        private Integer sortOrder;

        public ProductImageDtoBuilder id(Long id) { this.id = id; return this; }
        public ProductImageDtoBuilder url(String url) { this.url = url; return this; }
        public ProductImageDtoBuilder publicId(String publicId) { this.publicId = publicId; return this; }
        public ProductImageDtoBuilder isPrimary(Boolean isPrimary) { this.isPrimary = isPrimary; return this; }
        public ProductImageDtoBuilder sortOrder(Integer sortOrder) { this.sortOrder = sortOrder; return this; }

        public ProductImageDto build() {
            return new ProductImageDto(id, url, publicId, isPrimary, sortOrder);
        }
    }
}
