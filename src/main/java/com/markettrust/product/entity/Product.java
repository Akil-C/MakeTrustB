package com.markettrust.product.entity;

import com.markettrust.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "products", indexes = {
        @Index(name = "idx_product_seller_id", columnList = "seller_id"),
        @Index(name = "idx_product_category_id", columnList = "category_id"),
        @Index(name = "idx_product_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Product extends BaseEntity {

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "price_in_credits", nullable = false)
    private Long priceInCredits;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_condition", nullable = false, length = 30)
    private ProductCondition condition;

    @Column(name = "brand", length = 100)
    private String brand;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProductStatus status = ProductStatus.DRAFT;

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 1;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Enumerated(EnumType.STRING)
    @Column(name = "location_type", length = 20)
    private LocationType locationType = LocationType.APPROXIMATE;

    @Column(name = "views", nullable = false)
    private Integer views = 0;

    @Column(name = "unique_views", nullable = false)
    private Integer uniqueViews = 0;

    @Column(name = "wishlist_count", nullable = false)
    private Integer wishlistCount = 0;

    @Column(name = "inquiry_count", nullable = false)
    private Integer inquiryCount = 0;

    @Column(name = "is_featured", nullable = false)
    private Boolean isFeatured = false;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    public Long getSellerId() { return sellerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getPriceInCredits() { return priceInCredits; }
    public void setPriceInCredits(Long priceInCredits) { this.priceInCredits = priceInCredits; }

    public ProductCondition getCondition() { return condition; }
    public void setCondition(ProductCondition condition) { this.condition = condition; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public ProductStatus getStatus() { return status; }
    public void setStatus(ProductStatus status) { this.status = status; }

    public Integer getQuantity() { return quantity != null ? quantity : 1; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public LocationType getLocationType() { return locationType; }
    public void setLocationType(LocationType locationType) { this.locationType = locationType; }

    public Integer getViews() { return views != null ? views : 0; }
    public void setViews(Integer views) { this.views = views; }

    public Integer getUniqueViews() { return uniqueViews != null ? uniqueViews : 0; }
    public void setUniqueViews(Integer uniqueViews) { this.uniqueViews = uniqueViews; }

    public Integer getWishlistCount() { return wishlistCount != null ? wishlistCount : 0; }
    public void setWishlistCount(Integer wishlistCount) { this.wishlistCount = wishlistCount; }

    public Integer getInquiryCount() { return inquiryCount != null ? inquiryCount : 0; }
    public void setInquiryCount(Integer inquiryCount) { this.inquiryCount = inquiryCount; }

    public Boolean getIsFeatured() { return Boolean.TRUE.equals(isFeatured); }
    public void setIsFeatured(Boolean isFeatured) { this.isFeatured = isFeatured; }

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    private java.util.List<ProductImage> images = new java.util.ArrayList<>();

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public java.util.List<ProductImage> getImages() { return images != null ? images : java.util.Collections.emptyList(); }
    public void setImages(java.util.List<ProductImage> images) { this.images = images; }
}
