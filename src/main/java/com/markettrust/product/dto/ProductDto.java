package com.markettrust.product.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.markettrust.product.entity.LocationType;
import com.markettrust.product.entity.ProductCondition;
import com.markettrust.product.entity.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductDto {

    private Long             id;
    private String           title;
    private String           description;
    private Long             priceInCredits;
    private Long             categoryId;
    private String           categoryName;
    private ProductCondition condition;
    private String           brand;
    private ProductStatus    status;
    private Integer          quantity;
    private String           city;
    private String           state;
    private Double           latitude;
    private Double           longitude;
    private LocationType     locationType;
    private Integer          views;
    private Integer          uniqueViews;
    private Integer          wishlistCount;
    private Integer          inquiryCount;
    private Boolean          isFeatured;
    private List<ProductImageDto> images;
    private SellerSummaryDto seller;
    private Double           distance;
    private LocalDateTime    createdAt;
    private LocalDateTime    updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getPriceInCredits() { return priceInCredits; }
    public void setPriceInCredits(Long priceInCredits) { this.priceInCredits = priceInCredits; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public ProductCondition getCondition() { return condition; }
    public void setCondition(ProductCondition condition) { this.condition = condition; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public ProductStatus getStatus() { return status; }
    public void setStatus(ProductStatus status) { this.status = status; }

    public Integer getQuantity() { return quantity; }
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

    public Integer getViews() { return views; }
    public void setViews(Integer views) { this.views = views; }

    public Integer getUniqueViews() { return uniqueViews; }
    public void setUniqueViews(Integer uniqueViews) { this.uniqueViews = uniqueViews; }

    public Integer getWishlistCount() { return wishlistCount; }
    public void setWishlistCount(Integer wishlistCount) { this.wishlistCount = wishlistCount; }

    public Integer getInquiryCount() { return inquiryCount; }
    public void setInquiryCount(Integer inquiryCount) { this.inquiryCount = inquiryCount; }

    public Boolean getIsFeatured() { return isFeatured; }
    public void setIsFeatured(Boolean isFeatured) { this.isFeatured = isFeatured; }

    public List<ProductImageDto> getImages() { return images; }
    public void setImages(List<ProductImageDto> images) { this.images = images; }

    public SellerSummaryDto getSeller() { return seller; }
    public void setSeller(SellerSummaryDto seller) { this.seller = seller; }

    public Double getDistance() { return distance; }
    public void setDistance(Double distance) { this.distance = distance; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
